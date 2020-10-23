package jp.datachain.corda.ibc

import jp.datachain.corda.ibc.clients.corda.CordaClientState
import jp.datachain.corda.ibc.clients.corda.CordaCommitmentProof
import jp.datachain.corda.ibc.clients.corda.CordaConsensusState
import jp.datachain.corda.ibc.flows.*
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics20.Bank
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
import jp.datachain.corda.ibc.states.IbcState
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Version
import net.corda.core.contracts.StateRef
import net.corda.core.identity.Party
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode

class TestCordaIbcClient(val mockNet: MockNetwork, val mockNode: StartedMockNode) {
    var _baseId: StateRef? = null
    val baseId
        get() = _baseId!!

    inline fun host() = mockNode.services.vaultService.queryIbcHost(baseId)!!.state.data

    inline fun bank() = mockNode.services.vaultService.queryIbcBank(baseId)!!.state.data

    inline fun <reified T: IbcState> queryStateWithProof(id: Identifier): Pair<T, CordaCommitmentProof> {
        val stateAndRef = mockNode.services.vaultService.queryIbcState<T>(baseId!!, id)!!
        val stx = mockNode.services.validatedTransactions.getTransaction(stateAndRef.ref.txhash)!!
        val state = stateAndRef.state.data
        val proof = CordaCommitmentProof(
                stx.coreTransaction,
                stx.sigs.filter{it.by == mockNet.defaultNotaryIdentity.owningKey}.single()
        )
        return Pair(state, proof)
    }

    inline fun client(id: Identifier) = queryStateWithProof<ClientState>(id).first
    inline fun clientProof(id: Identifier) = queryStateWithProof<ClientState>(id).second

    inline fun conn(id: Identifier) = queryStateWithProof<Connection>(id).first
    inline fun connProof(id: Identifier) = queryStateWithProof<Connection>(id).second

    inline fun chan(id: Identifier) = queryStateWithProof<Channel>(id).first
    inline fun chanProof(id: Identifier) = queryStateWithProof<Channel>(id).second

    private fun <T> executeFlow(logic: net.corda.core.flows.FlowLogic<T>) : T {
        val future = mockNode.startFlow(logic)
        mockNet.runNetwork()
        return future.get()
    }

    fun createHost(participants: List<Party>) {
        assert(_baseId == null)

        val stxGenesis = executeFlow(IbcGenesisCreateFlow(
                participants
        ))
        _baseId = StateRef(stxGenesis.tx.id, 0)

        val stx = executeFlow(IbcHostAndBankCreateFlow(baseId))
        val host = stx.tx.outputsOfType<Host>().single()
        assert(host.baseId == baseId)
        val bank = stx.tx.outputsOfType<Bank>().single()
        assert(bank.baseId == baseId)
    }

    fun createClient(
            id: Identifier,
            clientType: ClientType,
            cordaConsensusState: CordaConsensusState
    ) {
        val stx = executeFlow(IbcClientCreateFlow(
                baseId,
                id,
                clientType,
                cordaConsensusState
        ))
        val client = stx.tx.outputsOfType<ClientState>().single()
        assert(client.id == id)
        assert(client is CordaClientState)
        assert((client as CordaClientState).consensusStates.values.single() == cordaConsensusState)
    }

    fun connOpenInit(
            identifier: Identifier,
            desiredConnectionIdentifier: Identifier,
            counterpartyPrefix: CommitmentPrefix,
            clientIdentifier: Identifier,
            counterpartyClientIdentifier: Identifier
    ) {
        val stx = executeFlow(IbcConnOpenInitFlow(
                baseId,
                identifier,
                desiredConnectionIdentifier,
                counterpartyPrefix,
                clientIdentifier,
                counterpartyClientIdentifier
        ))
        val conn = stx.tx.outputsOfType<Connection>().single()
        assert(conn.id == identifier)
        assert(conn.end.state == ConnectionState.INIT)
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
                baseId,
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
        val conn = stx.tx.outputsOfType<Connection>().single()
        assert(conn.id == desiredIdentifier)
        assert(conn.end.state == ConnectionState.TRYOPEN)
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
                baseId,
                identifier,
                version,
                proofTry,
                proofConsensus,
                proofHeight,
                consensusHeight
        ))
        val conn = stx.tx.outputsOfType<Connection>().single()
        assert(conn.id == identifier)
        assert(conn.end.state == ConnectionState.OPEN)
    }

    fun connOpenConfirm(
            identifier: Identifier,
            proofAck: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = executeFlow(IbcConnOpenConfirmFlow(
                baseId,
                identifier,
                proofAck,
                proofHeight
        ))
        val conn = stx.tx.outputsOfType<Connection>().single()
        assert(conn.id == identifier)
        assert(conn.end.state == ConnectionState.OPEN)
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
                baseId,
                order,
                connectionHops,
                portIdentifier,
                channelIdentifier,
                counterpartyPortIdentifier,
                counterpartyChannelIdentifier,
                version
        ))
        val chan = stx.tx.outputsOfType<Channel>().single()
        assert(chan.portId == portIdentifier)
        assert(chan.id == channelIdentifier)
        assert(chan.end.state == ChannelState.INIT)
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
                baseId,
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
        val chan = stx.tx.outputsOfType<Channel>().single()
        assert(chan.portId == portIdentifier)
        assert(chan.id == channelIdentifier)
        assert(chan.end.state == ChannelState.TRYOPEN)
    }

    fun chanOpenAck(
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyVersion: Version.Single,
            proofTry: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = executeFlow(IbcChanOpenAckFlow(
                baseId,
                portIdentifier,
                channelIdentifier,
                counterpartyVersion,
                proofTry,
                proofHeight
        ))
        val chan = stx.tx.outputsOfType<Channel>().single()
        assert(chan.portId == portIdentifier)
        assert(chan.id == channelIdentifier)
        assert(chan.end.state == ChannelState.OPEN)
    }

    fun chanOpenConfirm(
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            proofAck: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = executeFlow(IbcChanOpenConfirmFlow(
                baseId,
                portIdentifier,
                channelIdentifier,
                proofAck,
                proofHeight
        ))
        val chan = stx.tx.outputsOfType<Channel>().single()
        assert(chan.portId == portIdentifier)
        assert(chan.id == channelIdentifier)
        assert(chan.end.state == ChannelState.OPEN)
    }

    fun chanCloseInit(
            portIdentifier: Identifier,
            channelIdentifier: Identifier
    ) {
        val stx = executeFlow(IbcChanCloseInitFlow(
                baseId,
                portIdentifier,
                channelIdentifier
        ))
        val chan = stx.tx.outputsOfType<Channel>().single()
        assert(chan.portId == portIdentifier)
        assert(chan.id == channelIdentifier)
        assert(chan.end.state == ChannelState.CLOSED)
    }

    fun chanCloseConfirm(
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            proofInit: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = executeFlow(IbcChanCloseConfirmFlow(
                baseId,
                portIdentifier,
                channelIdentifier,
                proofInit,
                proofHeight
        ))
        val chan = stx.tx.outputsOfType<Channel>().single()
        assert(chan.portId == portIdentifier)
        assert(chan.id == channelIdentifier)
        assert(chan.end.state == ChannelState.CLOSED)
    }

    fun sendPacket(
            packet: Packet
    ) {
        val stx = executeFlow(IbcSendPacketFlow(baseId, packet))
        val chan = stx.tx.outputsOfType<Channel>().single()
        assert(chan.nextSequenceSend == packet.sequence + 1)
        assert(chan.packets[packet.sequence] == packet)
    }

    fun recvPacket(
            packet: Packet,
            proof: CommitmentProof,
            proofHeight: Height,
            acknowledgement: Acknowledgement
    ) {
        val stx = executeFlow(IbcRecvPacketFlow(
                baseId,
                packet,
                proof,
                proofHeight,
                acknowledgement
        ))
        val chan = stx.tx.outputsOfType<Channel>().single()
        assert(chan.nextSequenceRecv == packet.sequence + 1)
    }

    fun acknowledgePacket(
            packet: Packet,
            acknowledgement: Acknowledgement,
            proof: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = executeFlow(IbcAcknowledgePacketFlow(
                baseId,
                packet,
                acknowledgement,
                proof,
                proofHeight
        ))
        val chan = stx.tx.outputsOfType<Channel>().single()
        assert(chan.nextSequenceAck == packet.sequence + 1)
        assert(!chan.packets.contains(packet.sequence))
    }
}