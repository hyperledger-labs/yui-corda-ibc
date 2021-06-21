package jp.datachain.corda.ibc.flows.ics20cash

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.flows.util.queryIbcHost
import jp.datachain.corda.ibc.ics20cash.CashBank
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcCashBankCreateFlow(
        private val baseId: StateRef,
        private val bank: Party
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val builder = TransactionBuilder(notary)

        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants
        require(participants.contains(ourIdentity))
        require(participants.contains(bank))

        val newBank = CashBank(host.state.data, bank)
        val newHost = host.state.data.addBank(newBank.id)

        builder.addCommand(Ibc.Commands.CashBankCreate(bank), ourIdentity.owningKey)
                .addInputState(host)
                .addOutputState(newHost)
                .addOutputState(newBank)

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        return subFlow(FinalityFlow(tx, sessions))
    }
}

@InitiatedBy(IbcCashBankCreateFlow::class)
class IbcCashBankCreateResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}