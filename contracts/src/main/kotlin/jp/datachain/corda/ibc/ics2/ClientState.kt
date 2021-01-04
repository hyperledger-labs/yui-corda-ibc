package jp.datachain.corda.ibc.ics2

import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.client.v1.Client
import ibc.core.commitment.v1.Commitment
import ibc.core.connection.v1.Connection
import ics23.Proofs
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcState

interface ClientState : IbcState {
    fun clientType(): ClientType
    fun getLatestHeight(): Client.Height
    fun isFrozen(): Boolean
    fun getFrozenHeight(): Client.Height
    fun validate()
    fun getProofSpecs(): List<Proofs.ProofSpec>

    // Update and Misbehaviour functions

    fun checkHeaderAndUpdateState(header: Header): Pair<ClientState, ConsensusState>
    fun checkMisbehaviourAndUpdateState(misbehaviour: Misbehaviour): ClientState
    fun checkProposedHeaderAndUpdateState(header: Header): Pair<ClientState, ConsensusState>

    // Upgrade functions
    fun verifyUpgrade(
            newClient: ClientState,
            upgradeHeight: Client.Height,
            proofUpgrade: ByteArray
    )

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
            clientState: ClientState
    )

    fun verifyClientConsensusState(
            height: Client.Height,
            counterpartyClientIdentifier: Identifier,
            consensusHeight: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            consensusState: ConsensusState
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
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            sequence: Long,
            commitmentBytes: ByteArray
    )

    fun verifyPacketAcknowledgement(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            sequence: Long,
            acknowledgement: ChannelOuterClass.Acknowledgement
    )

    fun verifyPacketReceiptAbsence(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            sequence: Long
    )

    fun verifyNextSequenceRecv(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            nextSequenceRecv: Long
    )
}