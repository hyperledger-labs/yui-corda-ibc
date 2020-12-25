package jp.datachain.corda.ibc.ics2

import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.client.v1.Client.Height
import ibc.core.commitment.v1.Commitment
import ibc.core.connection.v1.Connection
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcState

interface ClientState : IbcState {
    val consensusStates: Map<Height, ConsensusState>
    val connIds: List<Identifier>

    fun addConnection(id: Identifier) : ClientState

    fun checkValidityAndUpdateState(header: Header) : ClientState
    fun checkMisbehaviourAndUpdateState(evidence: Evidence) : ClientState

    fun latestClientHeight() : Height

    fun verifyClientConsensusState(
            height: Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            clientIdentifier: Identifier,
            consensusStateHeight: Height,
            consensusState: ConsensusState) : Boolean

    fun verifyConnectionState(
            height: Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            connectionIdentifier: Identifier,
            connectionEnd: Connection.ConnectionEnd) : Boolean

    fun verifyChannelState(
            height: Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            channelEnd: ChannelOuterClass.Channel) : Boolean

    fun verifyPacketData(
            height: Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            sequence: Long,
            packet: ChannelOuterClass.Packet) : Boolean

    fun verifyPacketAcknowledgement(
            height: Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            sequence: Long,
            acknowledgement: ChannelOuterClass.Acknowledgement) : Boolean

    fun verifyPacketAcknowledgementAbsence(
            height: Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            sequence: Long) : Boolean

    fun verifyNextSequenceRecv(
            height: Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            nextSequenceRecv: Long) : Boolean
}