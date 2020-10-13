package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.ConsensusState
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler.createClient
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcClientCreateFlow(val id: Identifier, val clientType: ClientType, val consensusState: ConsensusState) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val builder = TransactionBuilder(notary)

        val host = serviceHub.vaultService.queryHost(id.toUniqueIdentifier().externalId!!)
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        val (newHost, newClient) = host.state.data.createClient(id, clientType, consensusState)

        builder.addCommand(Ibc.Commands.ClientCreate(clientType, consensusState), ourIdentity.owningKey)
                .addInputState(host)
                .addOutputState(newHost)
                .addOutputState(newClient)

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        val stx = subFlow(FinalityFlow(tx, sessions))
        return stx
    }
}

@InitiatedBy(IbcClientCreateFlow::class)
class IbcClientCreateResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}