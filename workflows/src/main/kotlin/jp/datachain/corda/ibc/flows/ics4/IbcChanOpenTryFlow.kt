package jp.datachain.corda.ibc.flows.ics4

import co.paralleluniverse.fibers.Suspendable
import ibc.core.channel.v1.Tx
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.flows.util.queryIbcHost
import jp.datachain.corda.ibc.flows.util.queryIbcState
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.HandleChanOpenTry
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
class IbcChanOpenTryFlow(
        private val baseId: StateRef,
        private val msg: Tx.MsgChannelOpenTry
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        // query host from vault
        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        // query conn from vault
        val connId = Identifier(msg.channel.connectionHopsList.single())
        val conn = serviceHub.vaultService.queryIbcState<IbcConnection>(baseId, connId)!!

        // query client from vault
        val clientId = Identifier(conn.state.data.end.clientId)
        val client = serviceHub.vaultService.queryIbcState<IbcClientState>(baseId, clientId)!!

        // (optional) channel from vault
        val chanOrNull = if (msg.previousChannelId.isNotEmpty()) {
            serviceHub.vaultService.queryIbcState<IbcChannel>(baseId, Identifier(msg.previousChannelId))
        } else {
            null
        }

        // create command and outputs
        val handler = HandleChanOpenTry(msg)
        val inStates =
                if(chanOrNull == null)
                    setOf(host.state.data)
                else
                    setOf(host.state.data, chanOrNull.state.data)
        val ctx = Context(inStates, setOf(client, conn).map{it.state.data})
        val signers = listOf(ourIdentity.owningKey)
        handler.execute(ctx, signers)

        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val builder = TransactionBuilder(notary)
                .addCommand(Ibc.DatagramHandlerCommand.HandleChanOpenTry(handler), signers)
                .addReferenceState(ReferencedStateAndRef(client))
                .addReferenceState(ReferencedStateAndRef(conn))
                .addInputState(host)
        chanOrNull?.let{builder.addInputState(it)}
        ctx.outStates.forEach{builder.addOutputState(it)}

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        return subFlow(FinalityFlow(tx, sessions))
    }
}

@InitiatedBy(IbcChanOpenTryFlow::class)
class IbcChanOpenTryResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}