package jp.datachain.corda.ibc.flows.ics4

import co.paralleluniverse.fibers.Suspendable
import ibc.core.channel.v1.ChannelOuterClass
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.flows.util.queryIbcHost
import jp.datachain.corda.ibc.flows.util.queryIbcState
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.states.IbcChannel
import jp.datachain.corda.ibc.states.IbcConnection
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcSendPacketFlow(
        private val baseId: StateRef,
        private val packet: ChannelOuterClass.Packet
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val builder = TransactionBuilder(notary)

        // query host from vault
        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        // query chan from vault
        val chanId = Identifier(packet.sourceChannel)
        val chan = serviceHub.vaultService.queryIbcState<IbcChannel>(baseId, chanId)!!

        // query conn from vault
        val connId = Identifier(chan.state.data.end.connectionHopsList.single())
        val conn = serviceHub.vaultService.queryIbcState<IbcConnection>(baseId, connId)!!

        // query client from vault
        val clientId = Identifier(conn.state.data.end.clientId)
        val client = serviceHub.vaultService.queryIbcState<ClientState>(baseId, clientId)!!

        val ctx = Context(setOf(chan.state.data), setOf(host.state.data, client.state.data, conn.state.data))
        Handler.sendPacket(ctx, packet)

        builder.addCommand(Ibc.Commands.SendPacket(packet), ourIdentity.owningKey)
                .addReferenceState(ReferencedStateAndRef(host))
                .addReferenceState(ReferencedStateAndRef(client))
                .addReferenceState(ReferencedStateAndRef(conn))
                .addInputState(chan)
        ctx.outStates.forEach{builder.addOutputState(it)}

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        return subFlow(FinalityFlow(tx, sessions))
    }
}

@InitiatedBy(IbcSendPacketFlow::class)
class IbcSendPacketResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}