package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler.chanOpenConfirm
import jp.datachain.corda.ibc.states.Channel
import jp.datachain.corda.ibc.states.Connection
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Quadruple
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

object IbcChanOpenConfirmFlow {
    @StartableByRPC
    @InitiatingFlow
    class Initiator(
            val portIdentifier: Identifier,
            val channelIdentifier: Identifier,
            val proofAck: CommitmentProof,
            val proofHeight: Height
    ) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities.single()

            val builder = TransactionBuilder(notary)

            // query host from vault
            val host = serviceHub.vaultService.queryHost(channelIdentifier.toUniqueIdentifier().externalId!!)
            val participants = host.state.data.participants.map{it as Party}
            require(participants.contains(ourIdentity))

            // query chan from vault
            val chan = serviceHub.vaultService.queryBy<Channel>(
                    QueryCriteria.LinearStateQueryCriteria(linearId = listOf(channelIdentifier.toUniqueIdentifier()))
            ).states.single()

            // query conn from vault
            val connId = chan.state.data.end.connectionHops.single()
            val conn = serviceHub.vaultService.queryBy<Connection>(
                    QueryCriteria.LinearStateQueryCriteria(linearId = listOf(connId.toUniqueIdentifier()))
            ).states.single()

            // query client from vault
            val clientId = conn.state.data.end.clientIdentifier
            val client = serviceHub.vaultService.queryBy<ClientState>(
                    QueryCriteria.LinearStateQueryCriteria(linearId = listOf(clientId.toUniqueIdentifier()))
            ).states.single()

            val newChan = Quadruple(host.state.data, client.state.data, conn.state.data, chan.state.data).chanOpenConfirm(
                    portIdentifier,
                    channelIdentifier,
                    proofAck,
                    proofHeight)

            builder.addCommand(Ibc.Commands.ChanOpenConfirm(
                    portIdentifier,
                    channelIdentifier,
                    proofAck,
                    proofHeight
            ), ourIdentity.owningKey)
                    .addReferenceState(ReferencedStateAndRef(host))
                    .addReferenceState(ReferencedStateAndRef(client))
                    .addReferenceState(ReferencedStateAndRef(conn))
                    .addInputState(chan)
                    .addOutputState(newChan)

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