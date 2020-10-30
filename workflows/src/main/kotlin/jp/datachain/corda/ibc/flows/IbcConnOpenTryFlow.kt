package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler.connOpenTry
import jp.datachain.corda.ibc.states.Connection
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Version
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcConnOpenTryFlow(
        val baseId: StateRef,
        val desiredIdentifier: Identifier,
        val counterpartyConnectionIdentifier: Identifier,
        val counterpartyPrefix: CommitmentPrefix,
        val counterpartyClientIdentifier: Identifier,
        val clientIdentifier: Identifier,
        val counterpartyVersions: List<Version>,
        val proofInit: CommitmentProof,
        val proofConsensus: CommitmentProof,
        val proofHeight: Height,
        val consensusHeight: Height
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val builder = TransactionBuilder(notary)

        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        val client = serviceHub.vaultService.queryIbcState<ClientState>(baseId, clientIdentifier)!!
        val connOrNull = serviceHub.vaultService.queryIbcState<Connection>(baseId, desiredIdentifier)

        val (newHost, newClient, newConn) = Triple(host.state.data, client.state.data, connOrNull?.state?.data).connOpenTry(
                desiredIdentifier,
                counterpartyConnectionIdentifier,
                counterpartyPrefix,
                counterpartyClientIdentifier,
                clientIdentifier,
                counterpartyVersions,
                proofInit,
                proofConsensus,
                proofHeight,
                consensusHeight)

        builder.addCommand(Ibc.Commands.ConnOpenTry(
                desiredIdentifier,
                counterpartyConnectionIdentifier,
                counterpartyPrefix,
                counterpartyClientIdentifier,
                clientIdentifier,
                counterpartyVersions,
                proofInit,
                proofConsensus,
                proofHeight,
                consensusHeight
        ), ourIdentity.owningKey)
                .addInputState(host)
                .addInputState(client)
                .addOutputState(newHost)
                .addOutputState(newClient)
                .addOutputState(newConn)
        connOrNull?.let{builder.addInputState(it)}

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        val stx = subFlow(FinalityFlow(tx, sessions))
        return stx
    }
}

@InitiatedBy(IbcConnOpenTryFlow::class)
class IbcConnOpenTryResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}