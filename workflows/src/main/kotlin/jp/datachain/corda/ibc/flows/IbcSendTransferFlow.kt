package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import ibc.core.client.v1.Client.Height
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.CreateOutgoingPacket
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.states.IbcChannel
import jp.datachain.corda.ibc.states.IbcConnection
import jp.datachain.corda.ibc.types.Timestamp
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.security.PublicKey

@StartableByRPC
@InitiatingFlow
class IbcSendTransferFlow(
        val baseId: StateRef,
        val denomination: Denom,
        val amount: Amount,
        val sender: PublicKey,
        val receiver: PublicKey,
        val destPort: Identifier,
        val destChannel: Identifier,
        val sourcePort: Identifier,
        val sourceChannel: Identifier,
        val timeoutHeight: Height,
        val timeoutTimestamp: Timestamp,
        val sequence: Long
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        // query host from vault
        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        // query chan from vault
        val chan = serviceHub.vaultService.queryIbcState<IbcChannel>(baseId, sourceChannel)!!

        // query conn from vault
        val connId = chan.state.data.end.connectionHops.single()
        val conn = serviceHub.vaultService.queryIbcState<IbcConnection>(baseId, connId)!!

        // query client from vault
        val clientId = conn.state.data.end.clientIdentifier
        val client = serviceHub.vaultService.queryIbcState<ClientState>(baseId, clientId)!!

        // query bank from vault
        val bank = serviceHub.vaultService.queryIbcBank(baseId)!!

        // execute command for obtaining outputs
        val ctx = Context(
                setOf(chan, bank).map{it.state.data},
                setOf(host, client, conn).map{it.state.data}
        )
        val signers = listOf(ourIdentity.owningKey)
        val command = CreateOutgoingPacket(
                denomination,
                amount,
                sender,
                receiver,
                destPort,
                destChannel,
                sourcePort,
                sourceChannel,
                timeoutHeight,
                timeoutTimestamp,
                sequence)
        command.execute(ctx, signers)

        // build transaction
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val builder = TransactionBuilder(notary)
        builder.addCommand(command, signers)
                .addReferenceState(ReferencedStateAndRef(host))
                .addReferenceState(ReferencedStateAndRef(client))
                .addReferenceState(ReferencedStateAndRef(conn))
                .addInputState(chan)
                .addInputState(bank)
        ctx.outStates.forEach { builder.addOutputState(it) }

        // sign transaction (by initiator)
        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        val stx = subFlow(FinalityFlow(tx, sessions))
        return stx
    }
}

@InitiatedBy(IbcSendTransferFlow::class)
class IbcSendTransferResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}