package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.clients.corda.CordaConsensusState
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics25.Handler.createClient
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Timestamp
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

object IbcClientCreateFlow {
    @StartableByRPC
    @InitiatingFlow
    class Initiator : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities.single()

            val builder = TransactionBuilder(notary)

            val host = serviceHub.vaultService.queryBy<Host>().states.first() // queryBy returns all unconsumed states by default
            val participants = host.state.data.participants.map{it as Party}
            require(participants.contains(ourIdentity))

            val clientId = host.state.data.generateIdentifier()
            val clientType = ClientType.CordaClient
            val consensusState = CordaConsensusState(Timestamp(123), Height(456), notary.owningKey)
            val (newHost, newClient) = host.state.data.createClient(clientId, clientType, consensusState)

            builder.addCommand(Ibc.Commands.ClientCreate(clientId, clientType, consensusState), ourIdentity.owningKey)
                    .addInputState(host)
                    .addOutputState(newHost)
                    .addOutputState(newClient)

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