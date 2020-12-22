package jp.datachain.corda.ibc.relayer

import ibc.core.client.v1.Client.Height
import jp.datachain.corda.ibc.clients.corda.CordaConsensusState
import jp.datachain.corda.ibc.clients.corda.toProof
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
import jp.datachain.corda.ibc.types.Version
import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.NetworkHostAndPort

class CordaIbcClient(host: String, port: Int) {
    val cordaRPCClient = CordaRPCClient(NetworkHostAndPort(host, port))
    var cordaRPCConnection: CordaRPCConnection? = null

    var host: Host? = null
    var client: Pair<ClientState, SignedTransaction>? = null
    val conns = mutableMapOf<Identifier, Pair<Connection, SignedTransaction>>()
    val chans = mutableMapOf<Identifier, Pair<Channel, SignedTransaction>>()

    fun start(username: String = "user1", password: String = "test") {
        cordaRPCConnection = cordaRPCClient.start(username, password)
    }
    fun ops() = cordaRPCConnection!!.proxy

    fun host() = host!!
    private fun insertHost(v: Host) { assert(host == null); host = v }
    private fun updateHost(v: Host) { assert(host != null); host = v }

    fun client() = client!!.first
    fun clientProof() = client!!.second.toProof()
    private fun insertClient(v: ClientState, stx: SignedTransaction) { assert(client == null); client = Pair(v, stx)}
    private fun updateClient(v: ClientState, stx: SignedTransaction) { assert(client != null); client = Pair(v, stx)}

    fun conn(id: Identifier) = conns[id]!!.first
    fun connProof(id: Identifier) = conns[id]!!.second.toProof()
    fun conn() = conns.values.single().first
    fun connProof() = conns.values.single().second.toProof()
    fun insertConn(v: Connection, stx: SignedTransaction) { assert(!conns.contains(v.id)); conns.put(v.id, Pair(v, stx))}
    fun updateConn(v: Connection, stx: SignedTransaction) { assert(conns.contains(v.id)); conns.put(v.id, Pair(v, stx))}

    fun chan(id: Identifier) = chans[id]!!.first
    fun chanProof(id: Identifier) = chans[id]!!.second.toProof()
    fun chan() = chans.values.single().first
    fun chanProof() = chans.values.single().second.toProof()
    fun insertChan(v: Channel, stx: SignedTransaction) { assert(!chans.contains(v.id)); chans.put(v.id, Pair(v, stx))}
    fun updateChan(v: Channel, stx: SignedTransaction) { assert(chans.contains(v.id)); chans.put(v.id, Pair(v, stx))}

    fun createHost(participantNames: List<String>) {
        val participants = participantNames.map{ops().partiesFromName(it, false).single()}

        val stxGenesis = ops().startFlow(
                ::IbcGenesisCreateFlow,
                participants
        ).returnValue.get()

        val genesisRef = StateRef(stxGenesis.tx.id, 0)

        val stxHost = ops().startFlow(::IbcHostAndBankCreateFlow, genesisRef).returnValue.get()
        val state = stxHost.tx.outputsOfType<Host>().single()
        insertHost(state)
    }

    fun createClient(
            id: Identifier,
            clientType: ClientType,
            cordaConsensusState: CordaConsensusState
    ) {
        val stx = ops().startFlow(
                ::IbcClientCreateFlow,
                host().baseId,
                id,
                clientType,
                cordaConsensusState).returnValue.get()

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
            counterpartyClientIdentifier: Identifier,
            version: Version?
    ) {
        val stx = ops().startFlowDynamic(
                IbcConnOpenInitFlow::class.java,
                host().baseId,
                identifier,
                desiredConnectionIdentifier,
                counterpartyPrefix,
                clientIdentifier,
                counterpartyClientIdentifier,
                version).returnValue.get()

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
            counterpartyChosenConnectionIdentifer: Identifier,
            counterpartyConnectionIdentifier: Identifier,
            counterpartyPrefix: CommitmentPrefix,
            counterpartyClientIdentifier: Identifier,
            clientIdentifier: Identifier,
            counterpartyVersions: List<Version>,
            proofInit: CommitmentProof,
            proofConsensus: CommitmentProof,
            proofHeight: Height,
            consensusHeight: Height
    ) {
        val stx = ops().startFlowDynamic(
                IbcConnOpenTryFlow::class.java,
                host().baseId,
                desiredIdentifier,
                counterpartyChosenConnectionIdentifer,
                counterpartyConnectionIdentifier,
                counterpartyPrefix,
                counterpartyClientIdentifier,
                clientIdentifier,
                counterpartyVersions,
                proofInit,
                proofConsensus,
                proofHeight,
                consensusHeight).returnValue.get()

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
            version: Version,
            counterpartyIdentifier: Identifier,
            proofTry: CommitmentProof,
            proofConsensus: CommitmentProof,
            proofHeight: Height,
            consensusHeight: Height
    ) {
        val stx = ops().startFlowDynamic(
                IbcConnOpenAckFlow::class.java,
                host().baseId,
                identifier,
                version,
                counterpartyIdentifier,
                proofTry,
                proofConsensus,
                proofHeight,
                consensusHeight).returnValue.get()
        val state = stx.tx.outputsOfType<Connection>().single()
        assert(state.end.state == ConnectionState.OPEN)
        updateConn(state, stx)
    }

    fun connOpenConfirm(
            identifier: Identifier,
            proofAck: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = ops().startFlow(
                ::IbcConnOpenConfirmFlow,
                host().baseId,
                identifier,
                proofAck,
                proofHeight).returnValue.get()
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
            version: Version
    ) {
        val stx = ops().startFlowDynamic(
                IbcChanOpenInitFlow::class.java,
                host().baseId,
                order,
                connectionHops,
                portIdentifier,
                channelIdentifier,
                counterpartyPortIdentifier,
                counterpartyChannelIdentifier,
                version).returnValue.get()

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
            counterpartyChosenChannelIdentifer: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: Version,
            counterpartyVersion: Version,
            proofInit: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = ops().startFlowDynamic(
                IbcChanOpenTryFlow::class.java,
                host().baseId,
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
                proofHeight).returnValue.get()

        val hostState = stx.tx.outputsOfType<Host>().single()
        updateHost(hostState)

        val chanState = stx.tx.outputsOfType<Channel>().single()
        assert(chanState.end.state == ChannelState.TRYOPEN)
        insertChan(chanState, stx)
    }

    fun chanOpenAck(
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyVersion: Version,
            counterpartyChannelIdentifier: Identifier,
            proofTry: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = ops().startFlowDynamic(
                IbcChanOpenAckFlow::class.java,
                host().baseId,
                portIdentifier,
                channelIdentifier,
                counterpartyVersion,
                counterpartyChannelIdentifier,
                proofTry,
                proofHeight).returnValue.get()
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
        val stx = ops().startFlow(
                ::IbcChanOpenConfirmFlow,
                host().baseId,
                portIdentifier,
                channelIdentifier,
                proofAck,
                proofHeight).returnValue.get()
        val state = stx.tx.outputsOfType<Channel>().single()
        assert(state.end.state == ChannelState.OPEN)
        updateChan(state, stx)
    }

    fun chanCloseInit(
            portIdentifier: Identifier,
            channelIdentifier: Identifier
    ) {
        val stx = ops().startFlow(
                ::IbcChanCloseInitFlow,
                host().baseId,
                portIdentifier,
                channelIdentifier).returnValue.get()
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
        val stx = ops().startFlow(
                ::IbcChanCloseConfirmFlow,
                host().baseId,
                portIdentifier,
                channelIdentifier,
                proofInit,
                proofHeight).returnValue.get()
        val state = stx.tx.outputsOfType<Channel>().single()
        assert(state.end.state == ChannelState.CLOSED)
        updateChan(state, stx)
    }

    fun sendPacket(
            packet: Packet
    ) {
        val stx = ops().startFlow(::IbcSendPacketFlow, host().baseId, packet).returnValue.get()
        val state = stx.tx.outputsOfType<Channel>().single()
        assert(state.nextSequenceSend == packet.sequence + 1)
        assert(state.packets[packet.sequence] == packet)
        updateChan(state, stx)
    }

    fun recvPacket(
            packet: Packet,
            proof: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = ops().startFlow(
                ::IbcRecvPacketFlow,
                host().baseId,
                packet,
                proof,
                proofHeight,
                false).returnValue.get()
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
        val stx = ops().startFlow(
                ::IbcAcknowledgePacketFlow,
                host().baseId,
                packet,
                acknowledgement,
                proof,
                proofHeight,
                false).returnValue.get()
        val state = stx.tx.outputsOfType<Channel>().single()
        assert(state.nextSequenceAck == packet.sequence + 1)
        assert(!state.packets.contains(packet.sequence))
        updateChan(state, stx)
    }
}
