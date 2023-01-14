package jp.datachain.corda.ibc.flows.ics20

import co.paralleluniverse.fibers.Suspendable
import ibc.applications.transfer.v1.Tx
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.conversion.pack
import jp.datachain.corda.ibc.flows.util.queryIbcBank
import jp.datachain.corda.ibc.flows.util.queryIbcHost
import jp.datachain.corda.ibc.flows.util.queryIbcState
import jp.datachain.corda.ibc.ics26.CreateOutgoingPacket
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.states.IbcChannel
import jp.datachain.corda.ibc.states.IbcClientState
import jp.datachain.corda.ibc.states.IbcConnection
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcSendTransferFlow(
        private val baseId: StateRef,
        private val msg: Tx.MsgTransfer
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        // query host from vault
        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        // query chan from vault
        val chan = serviceHub.vaultService.queryIbcState<IbcChannel>(baseId, Identifier(msg.sourceChannel))!!

        // query conn from vault
        val connId = Identifier(chan.state.data.end.connectionHopsList.single())
        val conn = serviceHub.vaultService.queryIbcState<IbcConnection>(baseId, connId)!!

        // query client from vault
        val clientId = Identifier(conn.state.data.end.clientId)
        val client = serviceHub.vaultService.queryIbcState<IbcClientState>(baseId, clientId)!!

        // query bank from vault
        val bank = serviceHub.vaultService.queryIbcBank(baseId)!!

        // execute command for obtaining outputs
        val ctx = Context(
                setOf(chan, bank).map{it.state.data},
                setOf(host, client, conn).map{it.state.data}
        )
        val signers = listOf(ourIdentity.owningKey)
        val handler = CreateOutgoingPacket(msg.pack())
        handler.execute(ctx, signers)

        // build transaction
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val builder = TransactionBuilder(notary)
        builder.addCommand(Ibc.DatagramHandlerCommand.CreateOutgoingPacket(handler), signers)
                .addReferenceState(ReferencedStateAndRef(host))
                .addReferenceState(ReferencedStateAndRef(client))
                .addReferenceState(ReferencedStateAndRef(conn))
                .addInputState(chan)
                .addInputState(bank)
        ctx.outStates.forEach { builder.addOutputState(it) }

        // sign transaction (by initiator)
        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        return subFlow(FinalityFlow(tx, sessions))
    }
}

@InitiatedBy(IbcSendTransferFlow::class)
class IbcSendTransferResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}