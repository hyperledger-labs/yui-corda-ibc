package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.HandleChanOpenTry
import jp.datachain.corda.ibc.ics4.ChannelOrder
import jp.datachain.corda.ibc.states.Channel
import jp.datachain.corda.ibc.states.Connection
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Version
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcChanOpenTryFlow(
        val baseId: StateRef,
        val order: ChannelOrder,
        val connectionHops: List<Identifier>,
        val portIdentifier: Identifier,
        val channelIdentifier: Identifier,
        val counterpartyChosenChannelIdentifer: Identifier,
        val counterpartyPortIdentifier: Identifier,
        val counterpartyChannelIdentifier: Identifier,
        val version: Version,
        val counterpartyVersion: Version,
        val proofInit: CommitmentProof,
        val proofHeight: Height
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        // query host from vault
        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        // query conn from vault
        val connId = connectionHops.single()
        val conn = serviceHub.vaultService.queryIbcState<Connection>(baseId, connId)!!

        // query client from vault
        val clientId = conn.state.data.end.clientIdentifier
        val client = serviceHub.vaultService.queryIbcState<ClientState>(baseId, clientId)!!

        // (optional) channel from vault
        val chanOrNull = serviceHub.vaultService.queryIbcState<Channel>(baseId, channelIdentifier)

        // create command and outputs
        val command = HandleChanOpenTry(
                order,
                connectionHops,
                portIdentifier,
                channelIdentifier,
                counterpartyChosenChannelIdentifer,
                counterpartyPortIdentifier,
                counterpartyChannelIdentifier,
                version,
                counterpartyVersion,
                proofInit,
                proofHeight)
        val inStates =
                if(chanOrNull == null)
                    setOf(host.state.data)
                else
                    setOf(host.state.data, chanOrNull.state.data)
        val ctx = Context(inStates, setOf(client, conn).map{it.state.data})
        val signers = listOf(ourIdentity.owningKey)
        command.execute(ctx, signers)

        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val builder = TransactionBuilder(notary)
                .addCommand(command, signers)
                .addReferenceState(ReferencedStateAndRef(client))
                .addReferenceState(ReferencedStateAndRef(conn))
                .addInputState(host)
        chanOrNull?.let{builder.addInputState(it)}
        ctx.outStates.forEach{builder.addOutputState(it)}

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        val stx = subFlow(FinalityFlow(tx, sessions))
        return stx
    }
}

@InitiatedBy(IbcChanOpenTryFlow::class)
class IbcChanOpenTryResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}