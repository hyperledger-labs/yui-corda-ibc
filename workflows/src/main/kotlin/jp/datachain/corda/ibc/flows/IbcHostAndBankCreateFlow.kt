package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics24.Host
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcHostAndBankCreateFlow(val baseId: StateRef) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val builder = TransactionBuilder(notary)

        val genesis = serviceHub.vaultService.queryIbcGenesis(baseId)!!
        val participants = genesis.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))
        val host = Host(genesis)
        val bank = Bank(genesis)

        builder.addCommand(Ibc.Commands.HostAndBankCreate(), ourIdentity.owningKey)
                .addInputState(genesis)
                .addOutputState(host)
                .addOutputState(bank)

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        val stx = subFlow(FinalityFlow(tx, sessions))
        return stx
    }
}

@InitiatedBy(IbcHostAndBankCreateFlow::class)
class IbcHostAndBankCreateResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}