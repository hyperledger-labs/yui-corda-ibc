package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.HandleChanCloseInit
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
class IbcChanCloseInitFlow(
        val baseId: StateRef,
        val portIdentifier: Identifier,
        val channelIdentifier: Identifier
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        // query host from vault
        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        // query channel from vault
        val chan = serviceHub.vaultService.queryIbcState<IbcChannel>(baseId, channelIdentifier)!!

        // query connection from vault
        val connId = chan.state.data.end.connectionHops.single()
        val conn = serviceHub.vaultService.queryIbcState<IbcConnection>(baseId, connId)!!

        // create command and outputs
        val command = HandleChanCloseInit(portIdentifier, channelIdentifier)
        val ctx = Context(setOf(chan.state.data), setOf(host, conn).map{it.state.data})
        val signers = listOf(ourIdentity.owningKey)
        command.execute(ctx, signers)

        // build tx
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val builder = TransactionBuilder(notary)
                .addCommand(command, signers)
                .addReferenceState(ReferencedStateAndRef(host))
                .addReferenceState(ReferencedStateAndRef(conn))
                .addInputState(chan)
        ctx.outStates.forEach{builder.addOutputState(it)}

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        val stx = subFlow(FinalityFlow(tx, sessions))
        return stx
    }
}

@InitiatedBy(IbcChanCloseInitFlow::class)
class IbcChanCloseInitResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}