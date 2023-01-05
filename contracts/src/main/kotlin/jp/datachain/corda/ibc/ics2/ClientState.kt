package jp.datachain.corda.ibc.ics2

import com.google.protobuf.Any
import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.client.v1.Client
import ibc.core.client.v1.Genesis
import ibc.core.commitment.v1.Commitment
import ibc.core.connection.v1.Connection
import ics23.Proofs
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier

// TODO: これを実装したLcpClientStateが必要
interface ClientState {
    val anyClientState: Any
    val anyConsensusStates : Map<Client.Height, Any>

    val consensusStates: Map<Client.Height, ConsensusState>

    fun clientType(): ClientType
    fun getLatestHeight(): Client.Height
    fun validate()
    fun getProofSpecs(): List<Proofs.ProofSpec>

    // Initialization function
    // Clients must validate the initial consensus state, and may store any client-specific metadata
    // necessary for correct light client operation
    fun initialize(consState: ConsensusState)

    // Status function
    // Clients must return their status. Only Active clients are allowed to process packets.
    fun status(): Status

    // Genesis function
    fun exportMetadata(): List<Genesis.GenesisMetadata>

    // Update and Misbehaviour functions

    fun checkHeaderAndUpdateState(header: Header): Pair<ClientState, ConsensusState>
    fun checkMisbehaviourAndUpdateState(misbehaviour: Misbehaviour): ClientState
    fun checkSubstituteAndUpdateState(substituteClient: ClientState): ClientState

    // Upgrade functions
    // NOTE: proof heights are not included as upgrade to a new revision is expected to pass only on the last
    // height committed by the current revision. Clients are responsible for ensuring that the planned last
    // height of the current revision is somehow encoded in the proof verification process.
    // This is to ensure that no premature upgrades occur, since upgrade plans committed to by the counterparty
    // may be cancelled or modified before the last planned height.
    fun verifyUpgradeAndUpdateState(
            newClient: ClientState,
            newConsState: ConsensusState,
            proofUpgradeClient: CommitmentProof,
            proofUpgradeConsState: CommitmentProof
    ): Pair<ClientState, ConsensusState>
    // Utility function that zeroes out any client customizable fields in client state
    // Ledger enforced fields are maintained while all custom fields are zero values
    // Used to verify upgrades
    fun zeroCustomFields(): ClientState

    // State verification functions

    fun verifyClientState(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            counterpartyClientIdentifier: Identifier,
            proof: CommitmentProof,
            clientState: Any
    )

    fun verifyClientConsensusState(
            height: Client.Height,
            counterpartyClientIdentifier: Identifier,
            consensusHeight: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            consensusState: Any
    )

    fun verifyConnectionState(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            connectionID: Identifier,
            connectionEnd: Connection.ConnectionEnd
    )

    fun verifyChannelState(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            channel: ChannelOuterClass.Channel
    )

    fun verifyPacketCommitment(
            height: Client.Height,
            delayTimePeriod: Long,
            delayBlockPeriod: Long,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            sequence: Long,
            commitmentBytes: ByteArray
    )

    fun verifyPacketAcknowledgement(
            height: Client.Height,
            delayTimePeriod: Long,
            delayBlockPeriod: Long,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            sequence: Long,
            acknowledgement: ChannelOuterClass.Acknowledgement
    )

    fun verifyPacketReceiptAbsence(
            height: Client.Height,
            delayTimePeriod: Long,
            delayBlockPeriod: Long,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            sequence: Long
    )

    fun verifyNextSequenceRecv(
            height: Client.Height,
            delayTimePeriod: Long,
            delayBlockPeriod: Long,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            nextSequenceRecv: Long
    )
}
