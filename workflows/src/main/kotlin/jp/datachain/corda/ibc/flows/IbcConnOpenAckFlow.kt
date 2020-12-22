package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import ibc.core.client.v1.Client.Height
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.HandleConnOpenAck
import jp.datachain.corda.ibc.states.Connection
import jp.datachain.corda.ibc.types.Version
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcConnOpenAckFlow(
        val baseId: StateRef,
        val identifier: Identifier,
        val version: Version,
        val counterpartyIdentifier: Identifier,
        val proofTry: CommitmentProof,
        val proofConsensus: CommitmentProof,
        val proofHeight: Height,
        val consensusHeight: Height
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        // query host state
        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        // query conn state
        val conn = serviceHub.vaultService.queryIbcState<Connection>(baseId, identifier)!!
        // query client state
        val client = serviceHub.vaultService.queryIbcState<ClientState>(baseId, conn.state.data.end.clientIdentifier)!!

        // create command and outputs
        val command = HandleConnOpenAck(
                identifier,
                version,
                counterpartyIdentifier,
                proofTry,
                proofConsensus,
                proofHeight,
                consensusHeight)
        val ctx = Context(setOf(conn.state.data), setOf(host.state.data, client.state.data))
        val signers = listOf(ourIdentity.owningKey)
        command.execute(ctx, signers)

        // build tx
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val builder = TransactionBuilder(notary)
                .addCommand(command, signers)
                .addReferenceState(ReferencedStateAndRef(host))
                .addReferenceState(ReferencedStateAndRef(client))
                .addInputState(conn)
        ctx.outStates.forEach{builder.addOutputState(it)}

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        val stx = subFlow(FinalityFlow(tx, sessions))
        return stx
    }
}

@InitiatedBy(IbcConnOpenAckFlow::class)
class IbcConnOpenAckResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}