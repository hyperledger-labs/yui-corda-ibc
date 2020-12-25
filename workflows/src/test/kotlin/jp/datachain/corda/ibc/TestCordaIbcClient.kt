package jp.datachain.corda.ibc

import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.client.v1.Client.Height
import ibc.core.commitment.v1.Commitment
import ibc.core.connection.v1.Connection
import ibc.core.connection.v1.Tx
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
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcChannel
import jp.datachain.corda.ibc.states.IbcConnection
import jp.datachain.corda.ibc.states.IbcState
import jp.datachain.corda.ibc.types.Timestamp
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

    fun conn(id: Identifier) = queryStateWithProof<IbcConnection>(id).first
    fun connProof(id: Identifier) = queryStateWithProof<IbcConnection>(id).second

    fun chan(id: Identifier) = queryStateWithProof<IbcChannel>(id).first
    fun chanProof(id: Identifier) = queryStateWithProof<IbcChannel>(id).second

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
            counterpartyPrefix: Commitment.MerklePrefix,
            clientIdentifier: Identifier,
            counterpartyClientIdentifier: Identifier,
            version: Connection.Version?
    ) {
        val msg = Tx.MsgConnectionOpenInit.newBuilder()
                .setClientId(clientIdentifier.id)
                .setConnectionId(identifier.id)
                .setCounterparty(Connection.Counterparty.newBuilder()
                        .setClientId(counterpartyClientIdentifier.id)
                        .setConnectionId(desiredConnectionIdentifier.id)
                        .setPrefix(counterpartyPrefix)
                        .build())
                .apply{if (version != null) setVersion(version)}
                .build()
        val stx = executeFlow(IbcConnOpenInitFlow(baseId, msg))
        val conn = stx.tx.outputsOfType<IbcConnection>().single()
        assert(conn.id == identifier)
        assert(conn.end.state == Connection.State.STATE_INIT)
    }

    fun connOpenTry(
            desiredIdentifier: Identifier,
            counterpartyChosenConnectionIdentifer: Identifier,
            counterpartyConnectionIdentifier: Identifier,
            counterpartyPrefix: Commitment.MerklePrefix,
            counterpartyClientIdentifier: Identifier,
            clientIdentifier: Identifier,
            counterpartyVersions: List<Connection.Version>,
            proofInit: CommitmentProof,
            proofConsensus: CommitmentProof,
            proofHeight: Height,
            consensusHeight: Height
    ) {
        val msg = Tx.MsgConnectionOpenTry.newBuilder()
                .setClientId(clientIdentifier.id)
                .setDesiredConnectionId(desiredIdentifier.id)
                .setCounterpartyChosenConnectionId(counterpartyChosenConnectionIdentifer.id)
                //.setClientState(...)
                .setCounterparty(Connection.Counterparty.newBuilder()
                        .setClientId(counterpartyClientIdentifier.id)
                        .setConnectionId(counterpartyConnectionIdentifier.id)
                        .setPrefix(counterpartyPrefix)
                        .build())
                .addAllCounterpartyVersions(counterpartyVersions)
                .setProofHeight(proofHeight)
                .setProofInit(proofInit.toByteString())
                //.setProofClient(...)
                .setProofConsensus(proofConsensus.toByteString())
                .setConsensusHeight(consensusHeight)
                .build()
        val stx = executeFlow(IbcConnOpenTryFlow(baseId, msg))
        val conn = stx.tx.outputsOfType<IbcConnection>().single()
        assert(conn.id == desiredIdentifier)
        assert(conn.end.state == Connection.State.STATE_TRYOPEN)
    }

    fun connOpenAck(
            identifier: Identifier,
            version: Connection.Version,
            counterpartyIdentifier: Identifier,
            proofTry: CommitmentProof,
            proofConsensus: CommitmentProof,
            proofHeight: Height,
            consensusHeight: Height
    ) {
        val msg = Tx.MsgConnectionOpenAck.newBuilder()
                .setConnectionId(identifier.id)
                .setCounterpartyConnectionId(counterpartyIdentifier.id)
                .setVersion(version)
                //.setClientState(...)
                .setProofHeight(proofHeight)
                .setProofTry(proofTry.toByteString())
                //.setProofClient(...)
                .setProofConsensus(proofConsensus.toByteString())
                .setConsensusHeight(consensusHeight)
                .build()
        val stx = executeFlow(IbcConnOpenAckFlow(baseId, msg))
        val conn = stx.tx.outputsOfType<IbcConnection>().single()
        assert(conn.id == identifier)
        assert(conn.end.state == Connection.State.STATE_OPEN)
    }

    fun connOpenConfirm(
            identifier: Identifier,
            proofAck: CommitmentProof,
            proofHeight: Height
    ) {
        val msg = Tx.MsgConnectionOpenConfirm.newBuilder()
                .setConnectionId(identifier.id)
                .setProofAck(proofAck.toByteString())
                .setProofHeight(proofHeight)
                .build()
        val stx = executeFlow(IbcConnOpenConfirmFlow(baseId, msg))
        val conn = stx.tx.outputsOfType<IbcConnection>().single()
        assert(conn.id == identifier)
        assert(conn.end.state == Connection.State.STATE_OPEN)
    }

    fun chanOpenInit(
            order: ChannelOuterClass.Order,
            connectionHops: List<Identifier>,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: String
    ) {
        val msg = ibc.core.channel.v1.Tx.MsgChannelOpenInit.newBuilder()
                .setPortId(portIdentifier.id)
                .setChannelId(channelIdentifier.id)
                .apply{with(channelBuilder){
                    setOrdering(order)
                    counterpartyBuilder.setPortId(counterpartyPortIdentifier.id)
                    counterpartyBuilder.setChannelId(counterpartyChannelIdentifier.id)
                    addAllConnectionHops(connectionHops.map { it.id })
                    setVersion(version)
                }}
                .build()
        val stx = executeFlow(IbcChanOpenInitFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.portId == portIdentifier)
        assert(chan.id == channelIdentifier)
        assert(chan.end.state == ChannelOuterClass.State.STATE_INIT)
    }

    fun chanOpenTry(
            order: ChannelOuterClass.Order,
            connectionHops: List<Identifier>,
            portIdentifier: Identifier,
            desiredChannelIdentifier: Identifier,
            counterpartyChosenChannelIdentifier: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: String,
            counterpartyVersion: String,
            proofInit: CommitmentProof,
            proofHeight: Height
    ) {
        val msg = ibc.core.channel.v1.Tx.MsgChannelOpenTry.newBuilder()
                .setPortId(portIdentifier.id)
                .setDesiredChannelId(desiredChannelIdentifier.id)
                .setCounterpartyChosenChannelId(counterpartyChosenChannelIdentifier.id)
                .apply{with(channelBuilder){
                    setOrdering(order)
                    counterpartyBuilder.setPortId(counterpartyPortIdentifier.id)
                    counterpartyBuilder.setChannelId(counterpartyChannelIdentifier.id)
                    addAllConnectionHops(connectionHops.map{it.id})
                    setVersion(version)
                }}
                .setCounterpartyVersion(counterpartyVersion)
                .setProofInit(proofInit.toByteString())
                .setProofHeight(proofHeight)
                .build()
        val stx = executeFlow(IbcChanOpenTryFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.portId == portIdentifier)
        assert(chan.id == desiredChannelIdentifier)
        assert(chan.end.state == ChannelOuterClass.State.STATE_TRYOPEN)
    }

    fun chanOpenAck(
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyVersion: String,
            counterpartyChannelIdentifier: Identifier,
            proofTry: CommitmentProof,
            proofHeight: Height
    ) {
        val msg = ibc.core.channel.v1.Tx.MsgChannelOpenAck.newBuilder()
                .setPortId(portIdentifier.id)
                .setChannelId(channelIdentifier.id)
                .setCounterpartyChannelId(counterpartyChannelIdentifier.id)
                .setCounterpartyVersion(counterpartyVersion)
                .setProofTry(proofTry.toByteString())
                .setProofHeight(proofHeight)
                .build()
        val stx = executeFlow(IbcChanOpenAckFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.portId == portIdentifier)
        assert(chan.id == channelIdentifier)
        assert(chan.end.state == ChannelOuterClass.State.STATE_OPEN)
    }

    fun chanOpenConfirm(
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            proofAck: CommitmentProof,
            proofHeight: Height
    ) {
        val msg = ibc.core.channel.v1.Tx.MsgChannelOpenConfirm.newBuilder()
                .setPortId(portIdentifier.id)
                .setChannelId(channelIdentifier.id)
                .setProofAck(proofAck.toByteString())
                .setProofHeight(proofHeight)
                .build()
        val stx = executeFlow(IbcChanOpenConfirmFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.portId == portIdentifier)
        assert(chan.id == channelIdentifier)
        assert(chan.end.state == ChannelOuterClass.State.STATE_OPEN)
    }

    fun chanCloseInit(
            portIdentifier: Identifier,
            channelIdentifier: Identifier
    ) {
        val msg = ibc.core.channel.v1.Tx.MsgChannelCloseInit.newBuilder()
                .setPortId(portIdentifier.id)
                .setChannelId(channelIdentifier.id)
                .build()
        val stx = executeFlow(IbcChanCloseInitFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.portId == portIdentifier)
        assert(chan.id == channelIdentifier)
        assert(chan.end.state == ChannelOuterClass.State.STATE_CLOSED)
    }

    fun chanCloseConfirm(
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            proofInit: CommitmentProof,
            proofHeight: Height
    ) {
        val msg = ibc.core.channel.v1.Tx.MsgChannelCloseConfirm.newBuilder()
                .setPortId(portIdentifier.id)
                .setChannelId(channelIdentifier.id)
                .setProofInit(proofInit.toByteString())
                .setProofHeight(proofHeight)
                .build()
        val stx = executeFlow(IbcChanCloseConfirmFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.portId == portIdentifier)
        assert(chan.id == channelIdentifier)
        assert(chan.end.state == ChannelOuterClass.State.STATE_CLOSED)
    }

    fun sendPacket(
            packet: ChannelOuterClass.Packet
    ) {
        val stx = executeFlow(IbcSendPacketFlow(baseId, packet))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.nextSequenceSend == packet.sequence + 1)
        assert(chan.packets[packet.sequence] == packet)
    }

    fun recvPacketOrdered(
            packet: ChannelOuterClass.Packet,
            proof: CommitmentProof,
            proofHeight: Height,
            forIcs20: Boolean = false
    ) {
        val msg = ibc.core.channel.v1.Tx.MsgRecvPacket.newBuilder()
                .setPacket(packet)
                .setProof(proof.toByteString())
                .setProofHeight(proofHeight)
                .build()
        val stx = executeFlow(IbcRecvPacketFlow(baseId, msg, forIcs20))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.nextSequenceRecv == packet.sequence + 1)
    }

    fun recvPacketUnordered(
            packet: ChannelOuterClass.Packet,
            proof: CommitmentProof,
            proofHeight: Height,
            forIcs20: Boolean = false
    ) {
        val msg = ibc.core.channel.v1.Tx.MsgRecvPacket.newBuilder()
                .setPacket(packet)
                .setProof(proof.toByteString())
                .setProofHeight(proofHeight)
                .build()
        val stx = executeFlow(IbcRecvPacketFlow(baseId, msg, forIcs20))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.nextSequenceRecv == 1L)
    }

    fun acknowledgePacketOrdered(
            packet: ChannelOuterClass.Packet,
            acknowledgement: ChannelOuterClass.Acknowledgement,
            proof: CommitmentProof,
            proofHeight: Height,
            forIcs20: Boolean = false
    ) {
        val msg = ibc.core.channel.v1.Tx.MsgAcknowledgement.newBuilder()
                .setPacket(packet)
                .setAcknowledgement(acknowledgement.toByteString())
                .setProof(proof.toByteString())
                .setProofHeight(proofHeight)
                .build()
        val stx = executeFlow(IbcAcknowledgePacketFlow(baseId, msg, forIcs20))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.nextSequenceAck == packet.sequence + 1)
        assert(!chan.packets.contains(packet.sequence))
    }

    fun acknowledgePacketUnordered(
            packet: ChannelOuterClass.Packet,
            acknowledgement: ChannelOuterClass.Acknowledgement,
            proof: CommitmentProof,
            proofHeight: Height,
            forIcs20: Boolean = false
    ) {
        val msg = ibc.core.channel.v1.Tx.MsgAcknowledgement.newBuilder()
                .setPacket(packet)
                .setAcknowledgement(acknowledgement.toByteString())
                .setProof(proof.toByteString())
                .setProofHeight(proofHeight)
                .build()
        val stx = executeFlow(IbcAcknowledgePacketFlow(baseId, msg, forIcs20))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
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
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.nextSequenceSend == sequence + 1)
        val packet = chan.packets[sequence]!!
        assert(Identifier(packet.destinationPort) == destPort)
        assert(Identifier(packet.destinationChannel) == destChannel)
        assert(Identifier(packet.sourcePort) == sourcePort)
        assert(Identifier(packet.sourceChannel) == sourceChannel)
        assert(packet.timeoutHeight == timeoutHeight)
        assert(packet.timeoutTimestamp == timeoutTimestamp.timestamp)
        assert(packet.sequence == sequence)
        val data = FungibleTokenPacketData.decode(packet.data.toByteArray())
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