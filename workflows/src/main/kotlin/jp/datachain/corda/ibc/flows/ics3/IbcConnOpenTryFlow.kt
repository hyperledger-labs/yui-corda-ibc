package jp.datachain.corda.ibc.flows.ics3

import co.paralleluniverse.fibers.Suspendable
import ibc.core.connection.v1.Tx
import jp.datachain.corda.ibc.flows.util.queryIbcHost
import jp.datachain.corda.ibc.flows.util.queryIbcState
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.HandleConnOpenTry
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
class IbcConnOpenTryFlow(
        private val baseId: StateRef,
        private val msg: Tx.MsgConnectionOpenTry
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        // query host state
        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        // query client state
        val client = serviceHub.vaultService.queryIbcState<IbcClientState>(baseId, Identifier(msg.clientId))!!

        // query conn state
        val connOrNull = if (msg.previousConnectionId.isNotEmpty()) {
            serviceHub.vaultService.queryIbcState<IbcConnection>(baseId, Identifier(msg.previousConnectionId))
        } else {
            null
        }

        val command = HandleConnOpenTry(msg)
        val inStates =
                if (connOrNull == null)
                    setOf(host.state.data)
                else
                    setOf(host.state.data, connOrNull.state.data)
        val ctx = Context(inStates, setOf(client.state.data))
        val signers = listOf(ourIdentity.owningKey)
        command.execute(ctx, signers)

        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val builder = TransactionBuilder(notary)
                .addCommand(command, signers)
                .addInputState(host)
                .addReferenceState(ReferencedStateAndRef(client))
        connOrNull?.let{builder.addInputState(it)}
        ctx.outStates.forEach{builder.addOutputState(it)}

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        return subFlow(FinalityFlow(tx, sessions))
    }
}

@InitiatedBy(IbcConnOpenTryFlow::class)
class IbcConnOpenTryResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}