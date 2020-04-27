package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics24.HostSeed
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

object IbcHostSeedCreateFlow {
    @StartableByRPC
    @InitiatingFlow
    class Initiator(val participants: List<Party>) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            require(participants.contains(ourIdentity))

            val notary = serviceHub.networkMapCache.notaryIdentities.single()

            val builder = TransactionBuilder(notary)

            builder.addCommand(Ibc.Commands.HostSeedCreate(), ourIdentity.owningKey)
                    .addOutputState(HostSeed(participants))

            val tx = serviceHub.signInitialTransaction(builder)

            val sessions = (participants - ourIdentity).map{initiateFlow(it)}
            val stx = subFlow(FinalityFlow(tx, sessions))
            return stx
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
            println(stx)
        }
    }
}