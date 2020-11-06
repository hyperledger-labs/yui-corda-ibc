package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.HandleChanOpenInit
import jp.datachain.corda.ibc.ics4.ChannelOrder
import jp.datachain.corda.ibc.states.Connection
import jp.datachain.corda.ibc.types.Version
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcChanOpenInitFlow(
        val baseId: StateRef,
        val order: ChannelOrder,
        val connectionHops: List<Identifier>,
        val portIdentifier: Identifier,
        val channelIdentifier: Identifier,
        val counterpartyPortIdentifier: Identifier,
        val counterpartyChannelIdentifier: Identifier,
        val version: Version
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        // query host from vault
        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        // query connection from vault
        val connId = connectionHops.single()
        val conn = serviceHub.vaultService.queryIbcState<Connection>(baseId, connId)!!

        // create command and outputs
        val command = HandleChanOpenInit(
                order,
                connectionHops,
                portIdentifier,
                channelIdentifier,
                counterpartyPortIdentifier,
                counterpartyChannelIdentifier,
                version)
        val ctx = Context(setOf(host.state.data), setOf(conn.state.data))
        val signers = listOf(ourIdentity.owningKey)
        command.execute(ctx, signers)

        // build tx
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val builder = TransactionBuilder(notary)
                .addCommand(command, signers)
                .addInputState(host)
                .addReferenceState(ReferencedStateAndRef(conn))
        ctx.outStates.forEach{builder.addOutputState(it)}

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        val stx = subFlow(FinalityFlow(tx, sessions))
        return stx
    }
}

@InitiatedBy(IbcChanOpenInitFlow::class)
class IbcChanOpenInitResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}