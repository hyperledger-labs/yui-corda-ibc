package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.HandleConnOpenInit
import jp.datachain.corda.ibc.types.Version
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcConnOpenInitFlow(
        val baseId: StateRef,
        val identifier: Identifier,
        val desiredConnectionIdentifier: Identifier,
        val counterpartyPrefix: CommitmentPrefix,
        val clientIdentifier: Identifier,
        val counterpartyClientIdentifier: Identifier,
        val version: Version.Single?
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        val client = serviceHub.vaultService.queryIbcState<ClientState>(baseId, clientIdentifier)!!

        val command = HandleConnOpenInit(
                identifier,
                desiredConnectionIdentifier,
                counterpartyPrefix,
                clientIdentifier,
                counterpartyClientIdentifier,
                version)
        val ctx = Context(setOf(host.state.data, client.state.data), emptySet())
        val signers = listOf(ourIdentity.owningKey)
        command.execute(ctx, signers)

        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val builder = TransactionBuilder(notary)
        builder.addCommand(command, ourIdentity.owningKey)
                .addInputState(host)
                .addInputState(client)
        ctx.outStates.forEach{builder.addOutputState(it)}

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        val stx = subFlow(FinalityFlow(tx, sessions))
        return stx
    }
}

@InitiatedBy(IbcConnOpenInitFlow::class)
class IbcConnOpenInitResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}