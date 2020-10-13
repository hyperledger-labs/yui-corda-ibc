package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler.connOpenInit
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcConnOpenInitFlow(
        val identifier: Identifier,
        val desiredConnectionIdentifier: Identifier,
        val counterpartyPrefix: CommitmentPrefix,
        val clientIdentifier: Identifier,
        val counterpartyClientIdentifier: Identifier
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val builder = TransactionBuilder(notary)

        val host = serviceHub.vaultService.queryHost(clientIdentifier.toUniqueIdentifier().externalId!!)
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        val client = serviceHub.vaultService.queryBy<ClientState>(
                QueryCriteria.LinearStateQueryCriteria(linearId = listOf(clientIdentifier.toUniqueIdentifier()))
        ).states.single()

        val (newHost, newClient, conn) = Pair(host.state.data, client.state.data).connOpenInit(
                identifier,
                desiredConnectionIdentifier,
                counterpartyPrefix,
                clientIdentifier,
                counterpartyClientIdentifier)

        builder.addCommand(Ibc.Commands.ConnOpenInit(identifier, desiredConnectionIdentifier, counterpartyPrefix, clientIdentifier, counterpartyClientIdentifier), ourIdentity.owningKey)
                .addInputState(host)
                .addInputState(client)
                .addOutputState(newHost)
                .addOutputState(newClient)
                .addOutputState(conn)

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        val stx = subFlow(FinalityFlow(tx, sessions))
        return stx
    }
}

@InitiatedBy(IbcConnOpenInitFlow::class)
class IbcConnOpenInitResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}