package jp.datachain.corda.ibc

import jp.datachain.corda.ibc.clients.corda.CordaClientState
import jp.datachain.corda.ibc.clients.corda.CordaConsensusState
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.flows.*
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics20.FungibleTokenPacketData
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
import jp.datachain.corda.ibc.types.Timestamp
import jp.datachain.corda.ibc.types.Version
import net.corda.core.contracts.StateRef
import net.corda.core.identity.Party
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import java.security.PublicKey

class TestCordaIbcClient(val mockNet: MockNetwork, val mockNode: StartedMockNode) {
    var _baseId: StateRef? = null
    val baseId
        get() = _baseId!!

    fun host() = mockNode.services.vaultService.queryIbcHost(baseId)!!.state.data

    fun bank() = mockNode.services.vaultService.queryIbcBank(baseId)!!.state.data

    inline fun <reified T: IbcState> queryStateWithProof(id: Identifier): Pair<T, CommitmentProof> {
        val stateAndRef = mockNode.services.vaultService.queryIbcState<T>(baseId, id)!!
        val stx = mockNode.services.validatedTransactions.getTransaction(stateAndRef.ref.txhash)!!
        val state = stateAndRef.state.data
        return Pair(state, stx.toProof())
    }

    fun client(id: Identifier) = queryStateWithProof<ClientState>(id).first
    fun clientProof(id: Identifier) = queryStateWithProof<ClientState>(id).second

    fun conn(id: Identifier) = queryStateWithProof<Connection>(id).first
    fun connProof(id: Identifier) = queryStateWithProof<Connection>(id).second

    fun chan(id: Identifier) = queryStateWithProof<Channel>(id).first
    fun chanProof(id: Identifier) = queryStateWithProof<Channel>(id).second

    private fun <T> executeFlow(logic: net.corda.core.flows.FlowLogic<T>) : T {
        val future = mockNode.startFlow(logic)
        mockNet.runNetwork()
        return future.get()
    }

    fun createHostAndBank(participants: List<Party>) {
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

    fun allocateFund(owner: PublicKey, denom: Denom, amount: Amount) {
        val orgAmount = bank().allocated.get(denom)?.get(owner) ?: Amount(0)
        val stx = executeFlow(IbcFundAllocateFlow(
                baseId,
                owner,
                denom,
                amount
        ))
        val bank = stx.tx.outputsOfType<Bank>().single()
        assert(bank.allocated[denom]!![owner]!! == orgAmount + amount)
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
            counterpartyClientIdentifier: Identifier,
            version: Version?
    ) {
        val stx = executeFlow(IbcConnOpenInitFlow(
                baseId,
                identifier,
                desiredConnectionIdentifier,
                counterpartyPrefix,
                clientIdentifier,
                counterpartyClientIdentifier,
                version
        ))
        val conn = stx.tx.outputsOfType<Connection>().single()
        assert(conn.id == identifier)
        assert(conn.end.state == ConnectionState.INIT)
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
        val stx = executeFlow(IbcConnOpenTryFlow(
                baseId,
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
                consensusHeight
        ))
        val conn = stx.tx.outputsOfType<Connection>().single()
        assert(conn.id == desiredIdentifier)
        assert(conn.end.state == ConnectionState.TRYOPEN)
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
        val stx = executeFlow(IbcConnOpenAckFlow(
                baseId,
                identifier,
                version,
                counterpartyIdentifier,
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
            version: Version
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
            counterpartyChosenChannelIdentifer: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: Version,
            counterpartyVersion: Version,
            proofInit: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = executeFlow(IbcChanOpenTryFlow(
                baseId,
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
            counterpartyVersion: Version,
            counterpartyChannelIdentifier: Identifier,
            proofTry: CommitmentProof,
            proofHeight: Height
    ) {
        val stx = executeFlow(IbcChanOpenAckFlow(
                baseId,
                portIdentifier,
                channelIdentifier,
                counterpartyVersion,
                counterpartyChannelIdentifier,
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

    fun recvPacketOrdered(
            packet: Packet,
            proof: CommitmentProof,
            proofHeight: Height,
            forIcs20: Boolean = false
    ) {
        val stx = executeFlow(IbcRecvPacketFlow(
                baseId,
                packet,
                proof,
                proofHeight,
                forIcs20))
        val chan = stx.tx.outputsOfType<Channel>().single()
        assert(chan.nextSequenceRecv == packet.sequence + 1)
    }

    fun recvPacketUnordered(
            packet: Packet,
            proof: CommitmentProof,
            proofHeight: Height,
            forIcs20: Boolean = false
    ) {
        val stx = executeFlow(IbcRecvPacketFlow(
                baseId,
                packet,
                proof,
                proofHeight,
                forIcs20))
        val chan = stx.tx.outputsOfType<Channel>().single()
        assert(chan.nextSequenceRecv == 1L)
    }

    fun acknowledgePacketOrdered(
            packet: Packet,
            acknowledgement: Acknowledgement,
            proof: CommitmentProof,
            proofHeight: Height,
            forIcs20: Boolean = false
    ) {
        val stx = executeFlow(IbcAcknowledgePacketFlow(
                baseId,
                packet,
                acknowledgement,
                proof,
                proofHeight,
                forIcs20))
        val chan = stx.tx.outputsOfType<Channel>().single()
        assert(chan.nextSequenceAck == packet.sequence + 1)
        assert(!chan.packets.contains(packet.sequence))
    }

    fun acknowledgePacketUnordered(
            packet: Packet,
            acknowledgement: Acknowledgement,
            proof: CommitmentProof,
            proofHeight: Height,
            forIcs20: Boolean = false
    ) {
        val stx = executeFlow(IbcAcknowledgePacketFlow(
                baseId,
                packet,
                acknowledgement,
                proof,
                proofHeight,
                forIcs20))
        val chan = stx.tx.outputsOfType<Channel>().single()
        assert(chan.nextSequenceAck == 1L)
        assert(!chan.packets.contains(packet.sequence))
    }

    fun sendTransfer(
            denomination: Denom,
            amount: Amount,
            sender: PublicKey,
            receiver: PublicKey,
            destPort: Identifier,
            destChannel: Identifier,
            sourcePort: Identifier,
            sourceChannel: Identifier,
            timeoutHeight: Height,
            timeoutTimestamp: Timestamp,
            sequence: Long
    ) {
        val prevBank = bank()
        val stx = executeFlow(IbcSendTransferFlow(
                baseId,
                denomination,
                amount,
                sender,
                receiver,
                destPort,
                destChannel,
                sourcePort,
                sourceChannel,
                timeoutHeight,
                timeoutTimestamp,
                sequence
        ))
        val chan = stx.tx.outputsOfType<Channel>().single()
        assert(chan.nextSequenceSend == sequence + 1)
        val packet = chan.packets[sequence]!!
        assert(packet.destPort == destPort)
        assert(packet.destChannel == destChannel)
        assert(packet.sourcePort == sourcePort)
        assert(packet.sourceChannel == sourceChannel)
        assert(packet.timeoutHeight == timeoutHeight)
        assert(packet.timeoutTimestamp == timeoutTimestamp)
        assert(packet.sequence == sequence)
        val data = FungibleTokenPacketData.decode(packet.data.bytes)
        assert(data.denomination == denomination)
        assert(data.amount == amount)
        assert(data.sender == sender)
        assert(data.receiver == receiver)
        val bank = stx.tx.outputsOfType<Bank>().single()
        if (denomination.hasPrefix(sourcePort, sourceChannel)) {
            assert(prevBank.burn(sender, denomination.removePrefix(), amount) == bank)
        } else {
            assert(prevBank.lock(sender, denomination, amount) == bank)
        }
    }
}