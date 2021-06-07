package jp.datachain.corda.ibc.flows.ics20

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.flows.util.queryIbcBank
import jp.datachain.corda.ibc.ics20.Address
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Denom
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcFundAllocateFlow(
        private val baseId: StateRef,
        private val owner: Address,
        private val denom: Denom,
        private val amount: Amount
): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        // query the current bank state
        val bank = serviceHub.vaultService.queryIbcBank(baseId)!!
        val participants = bank.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        // calculate the next bank state
        val newBank = bank.state.data.allocate(owner, denom, amount)

        // build transaction
        val builder = TransactionBuilder(notary)
        builder.addCommand(Ibc.Commands.FundAllocate(owner, denom, amount), ourIdentity.owningKey)
                .addInputState(bank)
                .addOutputState(newBank)

        // sign transaction (by initiator)
        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        return subFlow(FinalityFlow(tx, sessions))
    }
}

@InitiatedBy(IbcFundAllocateFlow::class)
class IbcFundAllocateResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}