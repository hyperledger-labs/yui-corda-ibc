package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler.chanOpenTry
import jp.datachain.corda.ibc.ics4.ChannelOrder
import jp.datachain.corda.ibc.states.Channel
import jp.datachain.corda.ibc.states.Connection
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Quadruple
import jp.datachain.corda.ibc.types.Version
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

object IbcChanOpenTryFlow {
    @StartableByRPC
    @InitiatingFlow
    class Initiator(
            val hostIdentifier: Identifier,
            val order: ChannelOrder,
            val connectionHops: List<Identifier>,
            val portIdentifier: Identifier,
            val channelIdentifier: Identifier,
            val counterpartyPortIdentifier: Identifier,
            val counterpartyChannelIdentifier: Identifier,
            val version: Version.Single,
            val counterpartyVersion: Version.Single,
            val proofInit: CommitmentProof,
            val proofHeight: Height
    ) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call() : SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities.single()

            val builder = TransactionBuilder(notary)

            // query host from vault
            val host = serviceHub.vaultService.queryBy<Host>(
                    QueryCriteria.LinearStateQueryCriteria(linearId = listOf(hostIdentifier.toUniqueIdentifier()))
            ).states.single()
            val participants = host.state.data.participants.map{it as Party}
            require(participants.contains(ourIdentity))

            // query conn from vault
            val connId = connectionHops.single()
            val conn = serviceHub.vaultService.queryBy<Connection>(
                    QueryCriteria.LinearStateQueryCriteria(linearId = listOf(connId.toUniqueIdentifier()))
            ).states.single()

            // query client from vault
            val clientId = conn.state.data.end.clientIdentifier
            val client = serviceHub.vaultService.queryBy<ClientState>(
                    QueryCriteria.LinearStateQueryCriteria(linearId = listOf(clientId.toUniqueIdentifier()))
            ).states.single()

            // (optional) channel from vault
            val chans = serviceHub.vaultService.queryBy<Channel>(
                    QueryCriteria.LinearStateQueryCriteria(participants, listOf(channelIdentifier.toUniqueIdentifier()))
            ).states
            require(chans.size <= 1)
            val chan = chans.singleOrNull()

            val (newHost, newChan) = Quadruple(host.state.data, client.state.data, conn.state.data, chan?.state?.data).chanOpenTry(
                    order,
                    connectionHops,
                    portIdentifier,
                    channelIdentifier,
                    counterpartyPortIdentifier,
                    counterpartyChannelIdentifier,
                    version,
                    counterpartyVersion,
                    proofInit,
                    proofHeight)

            builder.addCommand(Ibc.Commands.ChanOpenTry(
                    order,
                    connectionHops,
                    portIdentifier,
                    channelIdentifier,
                    counterpartyPortIdentifier,
                    counterpartyChannelIdentifier,
                    version,
                    counterpartyVersion,
                    proofInit,
                    proofHeight
            ), ourIdentity.owningKey)
                    .addReferenceState(ReferencedStateAndRef(client))
                    .addReferenceState(ReferencedStateAndRef(conn))
                    .addInputState(host)
                    .addOutputState(newHost)
                    .addOutputState(newChan)
            chan?.let{builder.addInputState(it)}

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