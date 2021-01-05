package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import ibc.core.client.v1.Client
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.HandleClientCreate
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcClientCreateFlow(
        val baseId: StateRef,
        val msg: Client.MsgCreateClient
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        val command = HandleClientCreate(msg)
        val ctx = Context(setOf(host.state.data), setOf())
        val signers = listOf(ourIdentity.owningKey)
        command.execute(ctx, signers)

        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val builder = TransactionBuilder(notary)
                .addCommand(command, signers)
                .addInputState(host)
        ctx.outStates.forEach{builder.addOutputState(it)}

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        val stx = subFlow(FinalityFlow(tx, sessions))
        return stx
    }
}

@InitiatedBy(IbcClientCreateFlow::class)
class IbcClientCreateResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}