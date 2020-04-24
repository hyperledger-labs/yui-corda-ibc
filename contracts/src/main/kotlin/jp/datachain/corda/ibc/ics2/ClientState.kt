package jp.datachain.corda.ibc.ics2

import jp.datachain.corda.ibc.ics4.ChannelEnd
import jp.datachain.corda.ibc.ics3.ConnectionEnd
import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.ics4.Packet
import jp.datachain.corda.ibc.states.IbcState
import jp.datachain.corda.ibc.types.Height

interface ClientState : IbcState {
    fun checkValidityAndUpdateState(header: Header) : ClientState
    fun checkMisbehaviourAndUpdateState(evidence: Evidence) : ClientState

    fun latestClientHeight() : Height

    fun verifyClientConsensusState(
            height: Height,
            prefix: CommitmentPrefix,
            proof: CommitmentProof,
            clientIdentifier: Identifier,
            consensusStateHeight: Height,
            consensusState: ConsensusState) : Boolean

    fun verifyConnectionState(
            height: Height,
            prefix: CommitmentPrefix,
            proof: CommitmentProof,
            connectionIdentifier: Identifier,
            connectionEnd: ConnectionEnd) : Boolean

    fun verifyChannelState(
            height: Height,
            prefix: CommitmentPrefix,
            proof: CommitmentProof,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            channelEnd: ChannelEnd) : Boolean

    fun verifyPacketData(
            height: Height,
            prefix: CommitmentPrefix,
            proof: CommitmentProof,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            sequence: Int,
            packet: Packet) : Boolean

    fun verifyPacketAcknowledgement(
            height: Height,
            prefix: CommitmentPrefix,
            proof: CommitmentProof,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            sequence: Int,
            acknowledgement: Acknowledgement) : Boolean

    fun verifyPacketAcknowledgementAbsence(
            height: Height,
            prefix: CommitmentPrefix,
            proof: CommitmentProof,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            sequence: Int) : Boolean

    fun verifyNextSequenceRecv(
            height: Height,
            prefix: CommitmentPrefix,
            proof: CommitmentProof,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            nextSequenceRecv: Int) : Boolean
}