package jp.datachain.corda.ibc.clients.corda

import com.google.protobuf.Any
import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.client.v1.Client
import ibc.core.commitment.v1.Commitment
import ibc.core.connection.v1.Connection
import ibc.lightclients.corda.v1.Corda
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.conversion.toCorda
import jp.datachain.corda.ibc.conversion.unpack
import jp.datachain.corda.ibc.ics2.*
import jp.datachain.corda.ibc.ics20.toCommitment
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcChannel
import jp.datachain.corda.ibc.states.IbcClientState
import jp.datachain.corda.ibc.states.IbcConnection
import jp.datachain.corda.ibc.states.IbcState
import net.corda.core.contracts.BelongsToContract
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.serialization.SerializationFactory

@BelongsToContract(Ibc::class)
data class CordaClientState constructor(
        override val anyClientState: Any,
        override val anyConsensusStates: Map<Client.Height, Any>
) : ClientState {
    override val consensusStates = anyConsensusStates.mapValues{kv -> CordaConsensusState(kv.value)}

    constructor(anyClientState: Any, anyConsensusState: Any) : this(anyClientState, mapOf(HEIGHT to anyConsensusState))

    private val counterpartyBaseId = anyClientState.unpack<Corda.ClientState>().baseId.toCorda()
    private val counterpartyNotaryKey = anyClientState.unpack<Corda.ClientState>().notaryKey.toCorda()

    override fun clientType() = ClientType.CordaClient
    override fun getLatestHeight() = HEIGHT
    override fun validate() {}
    override fun getProofSpecs() = throw NotImplementedError()

    override fun initialize(consState: ConsensusState) {
        require(consState == consensusStates[HEIGHT]!!)
    }

    override fun status() = Status.Active

    override fun exportMetadata() = throw NotImplementedError()

    override fun checkHeaderAndUpdateState(header: Header) = throw NotImplementedError()
    override fun checkMisbehaviourAndUpdateState(misbehaviour: Misbehaviour) = throw NotImplementedError()
    override fun checkSubstituteAndUpdateState(substituteClient: ClientState) = throw NotImplementedError()

    override fun verifyUpgradeAndUpdateState(newClient: ClientState, newConsState: ConsensusState, proofUpgradeClient: CommitmentProof, proofUpgradeConsState: CommitmentProof) = throw NotImplementedError()
    override fun zeroCustomFields() = throw NotImplementedError()

    private fun verifyHeight(height: Client.Height) {
        require(height == getLatestHeight()){"unmatched height: $height != ${getLatestHeight()}"}
    }

    private fun verifyNotaryKey(proof: CommitmentProof) {
        val notaryKey = proof.toSignedTransaction().notary!!.owningKey
        require(notaryKey == counterpartyNotaryKey){"unmatched notary key: $notaryKey != $counterpartyNotaryKey"}
    }

    // This code is a bit tricky ...
    // In contract contexts, as of transaction elements (commands/inputs/outputs/references, etc.),
    // only ones which are instantiated by the special classloader in the beginning of verification process are used.
    // Deserialization of such states (from SignedTransaction) still works, but it generates objects instantiated by
    // the default classloader. Even if two classes are loaded from same source, if different classloaders are used,
    // they are treated as completely different classes on JVM.
    // In this code, the class T is loaded by the special classloader, and so we have to tell deserialization process
    // to use the same classloader in order to get an instance of T.
    private inline fun <reified T: IbcState> extractState(proof: CommitmentProof): T {
        val attachmentsClassLoader = T::class.java.classLoader
        val serializers = listOf(
                "AcknowledgementSerializer",
                "AnySerializer",
                "ChannelSerializer",
                "ConnectionEndSerializer",
                "CordaClientStateSerializer",
                "CordaConsensusStateSerializer",
                "DenomTraceSerializer",
                "FabricClientStateSerializer",
                "FabricConsensusStateSerializer",
                "LcpClientStateSerializer",
                "LcpConsensusStateSerializer",
                "HeightSerializer",
                "MsgAcknowledgementSerializer",
                "MsgChannelCloseInitSerializer",
                "MsgChannelCloseConfirmSerializer",
                "MsgChannelOpenAckSerializer",
                "MsgChannelOpenConfirmSerializer",
                "MsgChannelOpenInitSerializer",
                "MsgChannelOpenTrySerializer",
                "MsgConnectionOpenAckSerializer",
                "MsgConnectionOpenConfirmSerializer",
                "MsgConnectionOpenInitSerializer",
                "MsgConnectionOpenTrySerializer",
                "MsgCreateClientSerializer",
                "MsgRecvPacketSerializer",
                "MsgTransferSerializer",
                "PacketSerializer"
        ).map{
            val className = "jp.datachain.corda.ibc.serialization.$it"
            val clazz = attachmentsClassLoader.loadClass(className)
            clazz.getDeclaredConstructor().newInstance() as SerializationCustomSerializer<*,*>
        }.toSet()
        val context = SerializationFactory.defaultFactory.defaultContext
                .withClassLoader(attachmentsClassLoader)
                .withLenientCarpenter()
                .withCustomSerializers(serializers)
        val outputs = SerializationFactory.defaultFactory.withCurrentContext(context) {
            proof.toSignedTransaction().coreTransaction.outputsOfType<T>()
        }
        return outputs.single()
    }

    override fun verifyClientState(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            counterpartyClientIdentifier: Identifier,
            proof: CommitmentProof,
            clientState: Any
    ) {
        verifyHeight(height)
        verifyNotaryKey(proof)

        val includedState = extractState<IbcClientState>(proof)
        require(includedState.baseId == counterpartyBaseId){"unmatched client base id: ${includedState.baseId} != $counterpartyBaseId"}
        require(includedState.id == counterpartyClientIdentifier){"unmatched client id: ${includedState.id} != $counterpartyClientIdentifier"}
        require(includedState.anyClientState == clientState){"unmatched client state: ${includedState.anyClientState} != $clientState"}
    }

    override fun verifyClientConsensusState(
            height: Client.Height,
            counterpartyClientIdentifier: Identifier,
            consensusHeight: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            consensusState: Any
    ) {
        verifyHeight(height)
        verifyNotaryKey(proof)

        val includedState = extractState<IbcClientState>(proof)
        require(includedState.baseId == counterpartyBaseId){"unmatched consensus base id: ${includedState.baseId} != $counterpartyBaseId"}
        require(includedState.id == counterpartyClientIdentifier){"unmatched consensus id: ${includedState.id} != $counterpartyClientIdentifier"}
        require(includedState.anyConsensusStates[consensusHeight] == consensusState){"unmatched consensus state: ${includedState.anyConsensusStates[consensusHeight]} != $consensusState"}
    }

    override fun verifyConnectionState(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            connectionID: Identifier,
            connectionEnd: Connection.ConnectionEnd
    ) {
        verifyHeight(height)
        verifyNotaryKey(proof)

        val includedState = extractState<IbcConnection>(proof)
        require(includedState.baseId == counterpartyBaseId){"unmatched connection base id: ${includedState.baseId} != $counterpartyBaseId"}
        require(includedState.id == connectionID){"unmatched connection id: ${includedState.id} != $connectionID"}
        require(includedState.end == connectionEnd){"unmatched connection state: ${includedState.end} != $connectionEnd"}
    }

    override fun verifyChannelState(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            channel: ChannelOuterClass.Channel
    ) {
        verifyHeight(height)
        verifyNotaryKey(proof)

        val includedState = extractState<IbcChannel>(proof)
        require(includedState.baseId == counterpartyBaseId){"unmatched channel base id: ${includedState.baseId} != $counterpartyBaseId"}
        require(includedState.portId == portID){"unmatched port id: ${includedState.portId} != $portID"}
        require(includedState.id == channelID){"unmatched channel id: ${includedState.id} != $channelID"}
        require(includedState.end == channel){"unmatched channel state: ${includedState.end} != $channel"}
    }

    override fun verifyPacketCommitment(
            height: Client.Height,
            delayTimePeriod: Long,
            delayBlockPeriod: Long,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            sequence: Long,
            commitmentBytes: ByteArray
    ) {
        verifyHeight(height)
        verifyNotaryKey(proof)

        require(delayTimePeriod == 0L)
        require(delayBlockPeriod == 0L)

        val includedState = extractState<IbcChannel>(proof)
        require(includedState.baseId == counterpartyBaseId){"unmatched channel base id: ${includedState.baseId} != $counterpartyBaseId"}
        require(includedState.portId == portID){"unmatched port id: ${includedState.portId} != $portID"}
        require(includedState.id == channelID){"unmatched channel id: ${includedState.id} != $channelID"}
        val includedPacket = includedState.packets[sequence]!!
        require(includedPacket.toCommitment().contentEquals(commitmentBytes)){"unmatched packet: ${includedPacket.toCommitment()} != $commitmentBytes"}
    }

    override fun verifyPacketAcknowledgement(
            height: Client.Height,
            delayTimePeriod: Long,
            delayBlockPeriod: Long,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            sequence: Long,
            acknowledgement: ChannelOuterClass.Acknowledgement
    ) {
        verifyHeight(height)
        verifyNotaryKey(proof)

        require(delayTimePeriod == 0L)
        require(delayBlockPeriod == 0L)

        val includedState = extractState<IbcChannel>(proof)
        require(includedState.baseId == counterpartyBaseId){"unmatched channel base id: ${includedState.baseId} != $counterpartyBaseId"}
        require(includedState.portId == portID){"unmatched port id: ${includedState.portId} != $portID"}
        require(includedState.id == channelID){"unmatched channel id: ${includedState.id} != $channelID"}
        val includedAck = includedState.acknowledgements[sequence]!!
        require(includedAck == acknowledgement){"unmatched ack: $includedAck != $acknowledgement"}
    }

    override fun verifyPacketReceiptAbsence(
            height: Client.Height,
            delayTimePeriod: Long,
            delayBlockPeriod: Long,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            sequence: Long
    ) {
        throw NotImplementedError()
    }

    override fun verifyNextSequenceRecv(
            height: Client.Height,
            delayTimePeriod: Long,
            delayBlockPeriod: Long,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            nextSequenceRecv: Long
    ) {
        verifyHeight(height)
        verifyNotaryKey(proof)

        require(delayTimePeriod == 0L)
        require(delayBlockPeriod == 0L)

        val includedState = extractState<IbcChannel>(proof)
        require(includedState.baseId == counterpartyBaseId){"unmatched channel base id: ${includedState.baseId} != $counterpartyBaseId"}
        require(includedState.portId == portID){"unmatched port id: ${includedState.portId} != $portID"}
        require(includedState.id == channelID){"unmatched channel id: ${includedState.id} != $channelID"}
        require(includedState.nextSequenceRecv == nextSequenceRecv){"unmatched next sequence recv: ${includedState.nextSequenceRecv} != $nextSequenceRecv"}
    }
}
