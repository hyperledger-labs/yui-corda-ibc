package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler.chanCloseInit
import jp.datachain.corda.ibc.states.Channel
import jp.datachain.corda.ibc.states.Connection
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

object IbcChanCloseInitFlow {
    @StartableByRPC
    @InitiatingFlow
    class Initiator(
            val hostIdentifier: Identifier,
            val portIdentifier: Identifier,
            val channelIdentifier: Identifier
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

            // query channel from vault
            val chan = serviceHub.vaultService.queryBy<Channel>(
                    QueryCriteria.LinearStateQueryCriteria(linearId = listOf(channelIdentifier.toUniqueIdentifier()))
            ).states.single()

            // query connection from vault
            val connId = chan.state.data.end.connectionHops.single()
            val conn = serviceHub.vaultService.queryBy<Connection>(
                    QueryCriteria.LinearStateQueryCriteria(linearId = listOf(connId.toUniqueIdentifier()))
            ).states.single()

            // calculate a newly created channel state and an updated host state
            val newChan = Triple(host.state.data, conn.state.data, chan.state.data).chanCloseInit(
                    portIdentifier,
                    channelIdentifier)

            // build tx
            builder.addCommand(Ibc.Commands.ChanCloseInit(
                    portIdentifier,
                    channelIdentifier
            ), ourIdentity.owningKey)
                    .addReferenceState(ReferencedStateAndRef(host))
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