package jp.datachain.corda.ibc.clients.fabric

import com.google.protobuf.Any
import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.client.v1.Client
import ibc.core.commitment.v1.Commitment
import ibc.core.connection.v1.Connection
import ibc.lightclients.fabric.v1.Fabric
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics2.*
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.StateRef
import net.corda.core.identity.AbstractParty

@BelongsToContract(Ibc::class)
data class FabricClientState private constructor(
        override val participants: List<AbstractParty>,
        override val baseId: StateRef,
        override val id: Identifier,
        val fabricClientState: Fabric.ClientState,
        val fabricConsensusState: FabricConsensusState
) : ClientState {
    override val clientState get() = Any.pack(fabricClientState)!!
    override val consensusStates get() = mapOf(getLatestHeight() to fabricConsensusState)

    constructor(host: Host, id: Identifier, fabricClientState: Fabric.ClientState, fabricConsensusState: Fabric.ConsensusState)
            : this(host.participants, host.baseId, id, fabricClientState, FabricConsensusState(fabricConsensusState))

    override fun clientType() = ClientType.FabricClient
    override fun getLatestHeight() = Client.Height.newBuilder().apply{
        versionNumber = 0
        versionHeight = fabricClientState.lastChaincodeHeader.sequence.value
    }.build()!!
    override fun isFrozen() = false
    override fun getFrozenHeight() = throw NotImplementedError()
    override fun validate() {}
    override fun getProofSpecs() = throw NotImplementedError()

    override fun checkHeaderAndUpdateState(header: Header): Pair<ClientState, ConsensusState> {
        throw NotImplementedError()
    }
    override fun checkMisbehaviourAndUpdateState(misbehaviour: Misbehaviour): ClientState {
        throw NotImplementedError()
    }
    override fun checkProposedHeaderAndUpdateState(header: Header): Pair<ClientState, ConsensusState> {
        throw NotImplementedError()
    }

    override fun verifyUpgrade(newClient: ClientState, upgradeHeight: Client.Height, proofUpgrade: ByteArray) {
        throw NotImplementedError()
    }

    override fun zeroCustomFields(): ClientState {
        throw NotImplementedError()
    }

    override fun verifyClientState(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            counterpartyClientIdentifier: Identifier,
            proof: CommitmentProof,
            clientState: ClientState
    ) {}

    override fun verifyClientConsensusState(
            height: Client.Height,
            counterpartyClientIdentifier: Identifier,
            consensusHeight: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            consensusState: ConsensusState
    ) {}

    override fun verifyConnectionState(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            connectionID: Identifier,
            connectionEnd: Connection.ConnectionEnd
    ) {}

    override fun verifyChannelState(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            channel: ChannelOuterClass.Channel
    ) {}

    override fun verifyPacketCommitment(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            sequence: Long,
            commitmentBytes: ByteArray
    ) {}

    override fun verifyPacketAcknowledgement(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            sequence: Long,
            acknowledgement: ChannelOuterClass.Acknowledgement
    ) {}

    override fun verifyPacketReceiptAbsence(
            height: Client.Height,
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
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            nextSequenceRecv: Long
    ) {}
}