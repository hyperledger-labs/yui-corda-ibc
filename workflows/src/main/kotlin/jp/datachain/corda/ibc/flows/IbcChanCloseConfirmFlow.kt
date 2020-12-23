package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import ibc.core.client.v1.Client.Height
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.HandleChanCloseConfirm
import jp.datachain.corda.ibc.states.IbcChannel
import jp.datachain.corda.ibc.states.IbcConnection
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcChanCloseConfirmFlow(
        val baseId: StateRef,
        val portIdentifier: Identifier,
        val channelIdentifier: Identifier,
        val proofInit: CommitmentProof,
        val proofHeight: Height
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        // query host from vault
        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        // query chan from vault
        val chan = serviceHub.vaultService.queryIbcState<IbcChannel>(baseId, channelIdentifier)!!

        // query conn from vault
        val connId = chan.state.data.end.connectionHops.single()
        val conn = serviceHub.vaultService.queryIbcState<IbcConnection>(baseId, connId)!!

        // query client from vault
        val clientId = Identifier(conn.state.data.end.clientId)
        val client = serviceHub.vaultService.queryIbcState<ClientState>(baseId, clientId)!!

        // create command and outputs
        val command = HandleChanCloseConfirm(
                portIdentifier,
                channelIdentifier,
                proofInit,
                proofHeight)
        val ctx = Context(setOf(chan.state.data), setOf(host, client, conn).map{it.state.data})
        val signers = listOf(ourIdentity.owningKey)
        command.execute(ctx, signers)

        // build tx
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val builder = TransactionBuilder(notary)
                .addCommand(command, signers)
                .addReferenceState(ReferencedStateAndRef(host))
                .addReferenceState(ReferencedStateAndRef(client))
                .addReferenceState(ReferencedStateAndRef(conn))
                .addInputState(chan)
        ctx.outStates.forEach{builder.addOutputState(it)}

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        val stx = subFlow(FinalityFlow(tx, sessions))
        return stx
    }
}

@InitiatedBy(IbcChanCloseConfirmFlow::class)
class IbcChanCloseConfirmResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}