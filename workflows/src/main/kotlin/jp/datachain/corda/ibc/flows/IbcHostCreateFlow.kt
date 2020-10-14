package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.HostSeed
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcHostCreateFlow(val baseId: StateRef) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val builder = TransactionBuilder(notary)

        val seed = serviceHub.vaultService.queryBy<HostSeed>(QueryCriteria.VaultQueryCriteria(stateRefs = listOf(baseId))).states.first() // queryBy returns all unconsumed states by default
        val participants = seed.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))
        val host = Host(seed)

        builder.addCommand(Ibc.Commands.HostCreate(), ourIdentity.owningKey)
                .addInputState(seed)
                .addOutputState(host)

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        val stx = subFlow(FinalityFlow(tx, sessions))
        return stx
    }
}

@InitiatedBy(IbcHostCreateFlow::class)
class IbcHostCreateResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}