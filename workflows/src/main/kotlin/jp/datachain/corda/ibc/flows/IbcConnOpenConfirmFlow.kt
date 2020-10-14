package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler.connOpenConfirm
import jp.datachain.corda.ibc.states.Connection
import jp.datachain.corda.ibc.types.Height
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcConnOpenConfirmFlow(
        val baseId: StateRef,
        val identifier: Identifier,
        val proofAck: CommitmentProof,
        val proofHeight: Height
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val builder = TransactionBuilder(notary)

        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        val conn = serviceHub.vaultService.queryIbcState<Connection>(baseId, identifier)!!
        val client = serviceHub.vaultService.queryIbcState<ClientState>(baseId, conn.state.data.end.clientIdentifier)!!

        val newConn = Triple(host.state.data, client.state.data, conn.state.data).connOpenConfirm(
                identifier,
                proofAck,
                proofHeight)

        builder.addCommand(Ibc.Commands.ConnOpenConfirm(
                identifier,
                proofAck,
                proofHeight
        ), ourIdentity.owningKey)
                .addReferenceState(ReferencedStateAndRef(host))
                .addReferenceState(ReferencedStateAndRef(client))
                .addInputState(conn)
                .addOutputState(newConn)

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        val stx = subFlow(FinalityFlow(tx, sessions))
        return stx
    }
}

@InitiatedBy(IbcConnOpenConfirmFlow::class)
class IbcConnOpenConfirmResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}