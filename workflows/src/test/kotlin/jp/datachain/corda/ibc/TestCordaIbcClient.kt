package jp.datachain.corda.ibc

import jp.datachain.corda.ibc.clients.corda.CordaCommitmentProof
import jp.datachain.corda.ibc.clients.corda.CordaConsensusState
import jp.datachain.corda.ibc.flows.*
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics3.ConnectionState
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.ics4.ChannelOrder
import jp.datachain.corda.ibc.ics4.ChannelState
import jp.datachain.corda.ibc.ics4.Packet
import jp.datachain.corda.ibc.states.Channel
import jp.datachain.corda.ibc.states.Connection
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Version
import net.corda.core.contracts.StateRef
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import java.util.*

class TestCordaIbcClient(val mockNet: MockNetwork, val mockNode: StartedMockNode) {
    var host: Host? = null
    var client: Pair<ClientState, SignedTransaction>? = null
    val conns = mutableMapOf<Identifier, Pair<Connection, SignedTransaction>>()
    val chans = mutableMapOf<Identifier, Pair<Channel, SignedTransaction>>()

    fun host() = host!!
    private fun insertHost(v: Host) { assert(host == null); host = v }
    private fun updateHost(v: Host) { assert(host != null); host = v }

    private fun makeProof(stx: SignedTransaction) = CordaCommitmentProof(
            stx.coreTransaction,
            stx.sigs.filter{it.by == host().notary.owningKey}.single())

    fun client() = client!!.first
    fun clientProof() = makeProof(client!!.second)
    private fun insertClient(v: ClientState, stx: SignedTransaction) { assert(client == null); client = Pair(v, stx)}
    private fun updateClient(v: ClientState, stx: SignedTransaction) { assert(client != null); client = Pair(v, stx)}

    fun conn() = conns.values.single().first
    fun connProof() = makeProof(conns.values.single().second)
    fun insertConn(v: Connection, stx: SignedTransaction) { assert(!conns.contains(v.id)); conns.put(v.id, Pair(v, stx))}
    fun updateConn(v: Connection, stx: SignedTransaction) { assert(conns.contains(v.id)); conns.put(v.id, Pair(v, stx))}

    fun chan() = chans.values.single().first
    fun chanProof() = makeProof(chans.values.single().second)
    fun insertChan(v: Channel, stx: SignedTransaction) { assert(!chans.contains(v.id)); chans.put(v.id, Pair(v, stx))}
    fun updateChan(v: Channel, stx: SignedTransaction) { assert(chans.contains(v.id)); chans.put(v.id, Pair(v, stx))}

    private fun <T> executeFlow(logic: net.corda.core.flows.FlowLogic<T>) : T {
        val future = mockNode.startFlow(logic)
        mockNet.runNetwork()
        return future.get()
    }

    fun createHost(participants: List<Party>, uuid: UUID = UUID.randomUUID()) {
        val stxSeed = executeFlow(IbcHostSeedCreateFlow(
                participants
        ))
        val seedRef = StateRef(stxSeed.tx.id, 0)

        val stxHost = executeFlow(IbcHostCreateFlow(
                seedRef,
                uuid
        ))
        val state = stxHost.tx.outputsOfType<Host>().single()
        insertHost(state)
    }

    fun createClient(
            id: Identifier,
            clientType: ClientType,
            cordaConsensusState: CordaConsensusState
    ) {
        val stx = executeFlow(IbcClientCreateFlow(
                id,
                clientType,
                cordaConsensusState
        ))

        val hostState = stx.tx.outputsOfType<Host>().single()
        updateHost(hostState)

        val clientState = stx.tx.outputsOfType<ClientState>().single()
        insertClient(clientState, stx)
    }

    fun connOpenInit(
            identifier: Identifier,
            desiredConnectionIdentifier: Identifier,
            counterpartyPrefix: CommitmentPrefix,
            clientIdentifier: Identifier,
            counterpartyClientIdentifier: Identifier
    ) {
        val stx = executeFlow(IbcConnOpenInitFlow(
                identifier,
                desiredConnectionIdentifier,
                counterpartyPrefix,
                clientIdentifier,
                counterpartyClientIdentifier
        ))

        val hostState = stx.tx.outputsOfType<Host>().single()
        updateHost(hostState)

        val clientState = stx.tx.outputsOfType<ClientState>().single()
        updateClient(clientState, stx)

        val connState = stx.tx.outputsOfType<Connection>().single()
        assert(connState.end.state == ConnectionState.INIT)
        insertConn(connState, stx)
    }

    fun connOpenTry(
            desiredIdentifier: Identifier,
            counterpartyConnectionIdentifier: Identifier,
            counterpartyPrefix: CommitmentPrefix,
            counterpartyClientIdentifier: Identifier,
            clientIdentifier: Identifier,
            counterpartyVersions: Version.Multiple,
            proofInit: CommitmentProof,
            proofConsensus: CommitmentProof,
            proofHeight: Height,
            consensusHeight: Height
    ) {
        val stx = executeFlow(IbcConnOpenTryFlow(
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
        ))

        val hostState = stx.tx.outputsOfType<Host>().single()
        updateHost(hostState)

        val clientState = stx.tx.outputsOfType<ClientState>().single()
        updateClient(clientState, stx)

        val connState = stx.tx.outputsOfType<Connection>().single()
        assert(connState.end.state == ConnectionState.TRYOPEN)
        insertConn(connState, stx)
    }

    fun connOpenAck(
            identifier: Identifier,
            version: Version.Single,
            proofTry: CommitmentProof,
            proofConsensus: CommitmentProof,
            proofHeight: Height,
            consensusHeight: Height
    ) {
        val stx = executeFlow(IbcConnOpenAckFlow(
                identifier,
                version,
                proofTry,
                proofConsensus,
                proofHeight,
                consensusHeight
        ))
        val state = stx.tx.outputsOfType<Connection>().single()
        assert(state.end.state == ConnectionState.OPEN)
        updateConn(state, stx)
    }

    fun connOpenConfirm(
            identifier: Identifier,
            proofAck: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = executeFlow(IbcConnOpenConfirmFlow(
                identifier,
                proofAck,
                proofHeight
        ))
        val state = stx.tx.outputsOfType<Connection>().single()
        assert(state.end.state == ConnectionState.OPEN)
        updateConn(state, stx)
    }

    fun chanOpenInit(
            order: ChannelOrder,
            connectionHops: List<Identifier>,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: Version.Single
    ) {
        val stx = executeFlow(IbcChanOpenInitFlow(
                order,
                connectionHops,
                portIdentifier,
                channelIdentifier,
                counterpartyPortIdentifier,
                counterpartyChannelIdentifier,
                version
        ))

        val hostState = stx.tx.outputsOfType<Host>().single()
        updateHost(hostState)

        val chanState = stx.tx.outputsOfType<Channel>().single()
        assert(chanState.end.state == ChannelState.INIT)
        insertChan(chanState, stx)
    }

    fun chanOpenTry(
            order: ChannelOrder,
            connectionHops: List<Identifier>,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: Version.Single,
            counterpartyVersion: Version.Single,
            proofInit: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = executeFlow(IbcChanOpenTryFlow(
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
        ))

        val hostState = stx.tx.outputsOfType<Host>().single()
        updateHost(hostState)

        val chanState = stx.tx.outputsOfType<Channel>().single()
        assert(chanState.end.state == ChannelState.TRYOPEN)
        insertChan(chanState, stx)
    }

    fun chanOpenAck(
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyVersion: Version.Single,
            proofTry: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = executeFlow(IbcChanOpenAckFlow(
                portIdentifier,
                channelIdentifier,
                counterpartyVersion,
                proofTry,
                proofHeight
        ))
        val state = stx.tx.outputsOfType<Channel>().single()
        assert(state.end.state == ChannelState.OPEN)
        updateChan(state, stx)
    }

    fun chanOpenConfirm(
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            proofAck: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = executeFlow(IbcChanOpenConfirmFlow(
                portIdentifier,
                channelIdentifier,
                proofAck,
                proofHeight
        ))
        val state = stx.tx.outputsOfType<Channel>().single()
        assert(state.end.state == ChannelState.OPEN)
        updateChan(state, stx)
    }

    fun chanCloseInit(
            portIdentifier: Identifier,
            channelIdentifier: Identifier
    ) {
        val stx = executeFlow(IbcChanCloseInitFlow(
                portIdentifier,
                channelIdentifier
        ))
        val state = stx.tx.outputsOfType<Channel>().single()
        assert(state.end.state == ChannelState.CLOSED)
        updateChan(state, stx)
    }

    fun chanCloseConfirm(
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            proofInit: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = executeFlow(IbcChanCloseConfirmFlow(
                portIdentifier,
                channelIdentifier,
                proofInit,
                proofHeight
        ))
        val state = stx.tx.outputsOfType<Channel>().single()
        assert(state.end.state == ChannelState.CLOSED)
        updateChan(state, stx)
    }

    fun sendPacket(
            packet: Packet
    ) {
        val stx = executeFlow(IbcSendPacketFlow(packet))
        val state = stx.tx.outputsOfType<Channel>().single()
        assert(state.nextSequenceSend == packet.sequence + 1)
        assert(state.packets[packet.sequence] == packet)
        updateChan(state, stx)
    }

    fun recvPacket(
            packet: Packet,
            proof: CommitmentProof,
            proofHeight: Height,
            acknowledgement: Acknowledgement
    ) {
        val stx = executeFlow(IbcRecvPacketFlow(
                packet,
                proof,
                proofHeight,
                acknowledgement
        ))
        val state = stx.tx.outputsOfType<Channel>().single()
        assert(state.nextSequenceRecv == packet.sequence + 1)
        updateChan(state, stx)
    }

    fun acknowledgePacket(
            packet: Packet,
            acknowledgement: Acknowledgement,
            proof: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = executeFlow(IbcAcknowledgePacketFlow(
                packet,
                acknowledgement,
                proof,
                proofHeight
        ))
        val state = stx.tx.outputsOfType<Channel>().single()
        assert(state.nextSequenceAck == packet.sequence + 1)
        assert(!state.packets.contains(packet.sequence))
        updateChan(state, stx)
    }
}
