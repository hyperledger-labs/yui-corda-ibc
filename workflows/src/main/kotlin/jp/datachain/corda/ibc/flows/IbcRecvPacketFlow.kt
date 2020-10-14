package jp.datachain.corda.ibc.flows

import co.paralleluniverse.fibers.Suspendable
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics25.Handler.recvPacket
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.ics4.Packet
import jp.datachain.corda.ibc.states.Channel
import jp.datachain.corda.ibc.states.Connection
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Quadruple
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcRecvPacketFlow(
        val baseId: StateRef,
        val packet: Packet,
        val proof: CommitmentProof,
        val proofHeight: Height,
        val acknowledgement: Acknowledgement
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
        val chanId = packet.destChannel
        val chan = serviceHub.vaultService.queryIbcState<Channel>(baseId, chanId)!!

        // query conn from vault
        val connId = chan.state.data.end.connectionHops.single()
        val conn = serviceHub.vaultService.queryIbcState<Connection>(baseId, connId)!!

        // query client from vault
        val clientId = conn.state.data.end.clientIdentifier
        val client = serviceHub.vaultService.queryIbcState<ClientState>(baseId, clientId)!!

        val newChan = Quadruple(host.state.data, client.state.data, conn.state.data, chan.state.data).recvPacket(
                packet,
                proof,
                proofHeight,
                acknowledgement)

        builder.addCommand(Ibc.Commands.RecvPacket(
                packet,
                proof,
                proofHeight,
                acknowledgement
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

@InitiatedBy(IbcRecvPacketFlow::class)
class IbcRecvPacketResponderFlow(val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}