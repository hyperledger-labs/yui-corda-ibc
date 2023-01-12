package jp.datachain.corda.ibc.flows.ics24

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.flows.util.queryIbcGenesis
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcHostCreateFlow(private val baseId: StateRef, private val moduleNames: Map<Identifier, String>) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val builder = TransactionBuilder(notary)

        val genesis = serviceHub.vaultService.queryIbcGenesis(baseId)!!
        val participants = genesis.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))
        val host = Host(genesis, moduleNames)

        builder.addCommand(Ibc.MiscCommands.HostCreate(moduleNames), ourIdentity.owningKey)
                .addInputState(genesis)
                .addOutputState(host)

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        return subFlow(FinalityFlow(tx, sessions))
    }
}

@InitiatedBy(IbcHostCreateFlow::class)
class IbcHostCreateResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}