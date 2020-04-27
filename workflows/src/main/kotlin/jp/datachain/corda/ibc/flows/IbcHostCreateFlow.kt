package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.HostSeed
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*
import kotlin.coroutines.experimental.coroutineContext

object IbcHostCreateFlow {
    @StartableByRPC
    @InitiatingFlow
    class Initiator : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities.single()

            val builder = TransactionBuilder(notary)

            val seed = serviceHub.vaultService.queryBy<HostSeed>().states.first() // queryBy returns all unconsumed states by default
            val participants = seed.state.data.participants.map{it as Party}
            require(participants.contains(ourIdentity))
            val uuid = UUID.randomUUID()
            val host = Host(seed, uuid)
            val host2 = Host(seed, uuid)
            require(host == host2){"host(${host}) != host2(${host2})"}

            builder.addCommand(Ibc.Commands.HostCreate(uuid), ourIdentity.owningKey)
                    .addInputState(seed)
                    .addOutputState(host)

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