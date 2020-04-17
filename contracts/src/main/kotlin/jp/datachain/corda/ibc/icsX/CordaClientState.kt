package jp.datachain.corda.ibc.icsX

import jp.datachain.corda.ibc.contracts.Tao
import jp.datachain.corda.ibc.ics2.*
import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics3.ConnectionEnd
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.ics4.ChannelEnd
import jp.datachain.corda.ibc.ics4.NextRecvSequence
import jp.datachain.corda.ibc.ics4.Packet
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Identifier
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import java.security.PublicKey

@BelongsToContract(Tao::class)
data class CordaClientState(
        val clientIdentifier: Identifier,
        val notaryKeys: Array<PublicKey>,
        val clientType: ClientType,
        val connectionIdentifiers: Collection<Identifier>
) : ClientState, ContractState {
    override val participants: List<AbstractParty> = listOf()

    init {
        assert(notaryKeys.size > 0)
    }

    companion object {
        fun initialise(
                clientIdentifier: Identifier,
                consensusState: CordaConsensusState
        ) = CordaClientState(
                clientIdentifier,
                arrayOf(consensusState.notaryKey),
                ClientType.CordaClient,
                setOf()
        )
    }

    override fun latestClientHeight() = Height(notaryKeys.size - 1)

    override fun checkValidityAndUpdateState(header: Header): ClientState {
        assert(header is CordaHeader)
        val header = header as CordaHeader

        // TODO: validate notary change transaction

        return this.copy(notaryKeys = notaryKeys + header.notaryChangeTx.newNotary.owningKey)
    }

    override fun checkMisbehaviourAndUpdateState(evidence: Evidence): ClientState {
        throw NotImplementedError()
    }

    override fun verifyClientConsensusState(height: Height, prefix: CommitmentPrefix, proof: CommitmentProof, clientIdentifier: Identifier, consensusStateHeight: Height, consensusState: ConsensusState): Boolean {
        assert(prefix is CordaCommitmentPrefix)
        val prefix = prefix as CordaCommitmentPrefix

        assert(proof is CordaCommitmentProof)
        val proof = proof as CordaCommitmentProof

        assert(consensusState is CordaConsensusState)
        val consensusState = consensusState as CordaConsensusState

        return prefix.stx.coreTransaction.outputStates.contains(consensusState) &&
                height.height <= latestClientHeight().height &&
                proof.signature.by.equals(notaryKeys[height.height]) &&
                proof.signature.verify(prefix.stx.id) &&
                consensusState.clientIdentifier == clientIdentifier &&
                consensusState.consensusStateHeight == consensusStateHeight
    }

    override fun verifyConnectionState(height: Height, prefix: CommitmentPrefix, proof: CommitmentProof, connectionIdentifier: Identifier, connectionEnd: ConnectionEnd): Boolean {
        assert(prefix is CordaCommitmentPrefix)
        val prefix = prefix as CordaCommitmentPrefix

        assert(proof is CordaCommitmentProof)
        val proof = proof as CordaCommitmentProof

        assert(connectionEnd is CordaConnectionEnd)
        val connectionEnd = connectionEnd as CordaConnectionEnd

        return prefix.stx.coreTransaction.outputStates.contains(connectionEnd) &&
                height.height <= latestClientHeight().height &&
                proof.signature.by.equals(notaryKeys[height.height]) &&
                proof.signature.verify(prefix.stx.id) &&
                connectionEnd.connectionIdentifier == connectionIdentifier
    }

    override fun verifyChannelState(height: Height, prefix: CommitmentPrefix, proof: CommitmentProof, portIdentifier: Identifier, channelIdentifier: Identifier, channelEnd: ChannelEnd): Boolean {
        assert(prefix is CordaCommitmentPrefix)
        val prefix = prefix as CordaCommitmentPrefix

        assert(proof is CordaCommitmentProof)
        val proof = proof as CordaCommitmentProof

        assert(channelEnd is CordaChannelEnd)
        val channelEnd = channelEnd as CordaChannelEnd

        return prefix.stx.coreTransaction.outputStates.contains(channelEnd) &&
                height.height <= latestClientHeight().height &&
                proof.signature.by.equals(notaryKeys[height.height]) &&
                proof.signature.verify(prefix.stx.id) &&
                channelEnd.portIdentifier == portIdentifier &&
                channelEnd.channelIdentifier == channelIdentifier
    }

    override fun verifyPacketData(height: Height, prefix: CommitmentPrefix, proof: CommitmentProof, portIdentifier: Identifier, channelIdentifier: Identifier, sequence: Int, packet: Packet): Boolean {
        assert(prefix is CordaCommitmentPrefix)
        val prefix = prefix as CordaCommitmentPrefix

        assert(proof is CordaCommitmentProof)
        val proof = proof as CordaCommitmentProof

        assert(packet is CordaPacket)
        val packet = packet as CordaPacket

        return prefix.stx.coreTransaction.outputStates.contains(packet) &&
                height.height <= latestClientHeight().height &&
                proof.signature.by.equals(notaryKeys[height.height]) &&
                proof.signature.verify(prefix.stx.id) &&
                packet.portIdentifier == portIdentifier &&
                packet.channelIdentifier == channelIdentifier &&
                packet.sequence == sequence
    }

    override fun verifyPacketAcknowledgement(height: Height, prefix: CommitmentPrefix, proof: CommitmentProof, portIdentifier: Identifier, channelIdentifier: Identifier, sequence: Int, acknowledgement: Acknowledgement): Boolean {
        assert(prefix is CordaCommitmentPrefix)
        val prefix = prefix as CordaCommitmentPrefix

        assert(proof is CordaCommitmentProof)
        val proof = proof as CordaCommitmentProof

        assert(acknowledgement is CordaAcknowledgement)
        val acknowledgement = acknowledgement as CordaAcknowledgement

        return prefix.stx.coreTransaction.outputStates.contains(acknowledgement) &&
                height.height <= latestClientHeight().height &&
                proof.signature.by.equals(notaryKeys[height.height]) &&
                proof.signature.verify(prefix.stx.id) &&
                acknowledgement.portIdentifier == portIdentifier &&
                acknowledgement.channelIdentifier == channelIdentifier &&
                acknowledgement.sequence == sequence
    }

    override fun verifyPacketAcknowledgementAbsence(height: Height, prefix: CommitmentPrefix, proof: CommitmentProof, portIdentifier: Identifier, channelIdentifier: Identifier, sequence: Int): Boolean {
        throw NotImplementedError()
    }

    override fun verifyNextSequenceRecv(height: Height, prefix: CommitmentPrefix, proof: CommitmentProof, portIdentifier: Identifier, channelIdentifier: Identifier, nextSequenceRecv: NextRecvSequence): Boolean {
        assert(prefix is CordaCommitmentPrefix)
        val prefix = prefix as CordaCommitmentPrefix

        assert(proof is CordaCommitmentProof)
        val proof = proof as CordaCommitmentProof

        assert(nextSequenceRecv is CordaNextRecvSequence)
        val nextSequenceRecv = nextSequenceRecv as CordaNextRecvSequence

        return prefix.stx.coreTransaction.outputStates.contains(nextSequenceRecv) &&
                height.height <= latestClientHeight().height &&
                proof.signature.by.equals(notaryKeys[height.height]) &&
                proof.signature.verify(prefix.stx.id) &&
                nextSequenceRecv.portIdentifier == portIdentifier &&
                nextSequenceRecv.channelIdentifier == channelIdentifier
    }
}