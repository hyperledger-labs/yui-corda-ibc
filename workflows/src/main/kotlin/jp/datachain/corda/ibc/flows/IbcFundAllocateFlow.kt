package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Denom
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.security.PublicKey

@StartableByRPC
@InitiatingFlow
class IbcFundAllocateFlow(
        val baseId: StateRef,
        val owner: PublicKey,
        val denom: Denom,
        val amount: Amount
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
        val stx = subFlow(FinalityFlow(tx, sessions))
        return stx
    }
}

@InitiatedBy(IbcFundAllocateFlow::class)
class IbcFundAllocateResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}