package jp.datachain.corda.ibc.clients.corda

import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics2.*
import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics3.ConnectionEnd
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.ics4.ChannelEnd
import jp.datachain.corda.ibc.ics4.Packet
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.Channel
import jp.datachain.corda.ibc.states.Connection
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import java.lang.IllegalArgumentException

@BelongsToContract(Ibc::class)
data class CordaClientState private constructor(
        override val participants: List<AbstractParty>,
        override val linearId: UniqueIdentifier,
        override val consensusStates: Map<Height, CordaConsensusState>,
        override val connIds: List<Identifier>
) : ClientState {
    constructor(host: Host, id: Identifier, consensusState: CordaConsensusState)
            : this(host.participants, id.toUniqueIdentifier(), mapOf(consensusState.height to consensusState), emptyList())

    private fun hostIdOf(height: Height) = consensusStates.get(height)?.hostId
    private fun notaryKeyOf(height: Height) = consensusStates.get(height)?.notaryKey

    override fun addConnection(id: Identifier): ClientState {
        require(validateIdentifier(id))
        require(!connIds.contains(id))
        return copy(connIds = connIds + id)
    }

    override fun latestClientHeight() = consensusStates.keys.single()

    override fun checkValidityAndUpdateState(header: Header): ClientState {
        throw NotImplementedError()
    }

    override fun checkMisbehaviourAndUpdateState(evidence: Evidence): ClientState {
        throw NotImplementedError()
    }

    override fun verifyClientConsensusState(height: Height, prefix: CommitmentPrefix, proof: CommitmentProof, clientIdentifier: Identifier, consensusStateHeight: Height, consensusState: ConsensusState): Boolean {
        val hostId = hostIdOf(height) ?: return false
        val notaryKey = notaryKeyOf(height) ?: return false
        val proof = proof as CordaCommitmentProof
        val consensusState = consensusState as CordaConsensusState
        val client = proof.tx.outputsOfType<CordaClientState>().singleOrNull() ?: return false

        return proof.sig.by == notaryKey &&
                proof.sig.verify(proof.tx.id) &&
                clientIdentifier.toUniqueIdentifier().externalId == hostId.toUniqueIdentifier().externalId &&
                client.id == clientIdentifier &&
                client.consensusStates.get(consensusStateHeight) == consensusState
    }

    override fun verifyConnectionState(height: Height, prefix: CommitmentPrefix, proof: CommitmentProof, connectionIdentifier: Identifier, connectionEnd: ConnectionEnd): Boolean {
        val hostId = hostIdOf(height) ?: return false
        val notaryKey = notaryKeyOf(height) ?: return false
        val proof = proof as CordaCommitmentProof
        val conn = proof.tx.outputsOfType<Connection>().singleOrNull() ?: return false

        return proof.sig.by == notaryKey &&
                proof.sig.verify(proof.tx.id) &&
                connectionIdentifier.toUniqueIdentifier().externalId == hostId.toUniqueIdentifier().externalId &&
                conn.id == connectionIdentifier &&
                conn.end == connectionEnd
    }

    override fun verifyChannelState(height: Height, prefix: CommitmentPrefix, proof: CommitmentProof, portIdentifier: Identifier, channelIdentifier: Identifier, channelEnd: ChannelEnd): Boolean {
        val hostId = hostIdOf(height) ?: return false
        val notaryKey = notaryKeyOf(height) ?: return false
        val proof = proof as CordaCommitmentProof
        val chan = proof.tx.outputsOfType<Channel>().singleOrNull() ?: return false

        return proof.sig.by == notaryKey &&
                proof.sig.verify(proof.tx.id) &&
                portIdentifier.toUniqueIdentifier().externalId == hostId.toUniqueIdentifier().externalId &&
                channelIdentifier.toUniqueIdentifier().externalId == hostId.toUniqueIdentifier().externalId &&
                chan.portId == portIdentifier &&
                chan.id == channelIdentifier &&
                chan.end == channelEnd
    }

    override fun verifyPacketData(height: Height, prefix: CommitmentPrefix, proof: CommitmentProof, portIdentifier: Identifier, channelIdentifier: Identifier, sequence: Long, packet: Packet): Boolean {
        val hostId = hostIdOf(height) ?: return false
        val notaryKey = notaryKeyOf(height) ?: return false
        val proof = proof as CordaCommitmentProof
        val chan = proof.tx.outputsOfType<Channel>().singleOrNull() ?: return false

        return proof.sig.by == notaryKey &&
                proof.sig.verify(proof.tx.id) &&
                portIdentifier.toUniqueIdentifier().externalId == hostId.toUniqueIdentifier().externalId &&
                channelIdentifier.toUniqueIdentifier().externalId == hostId.toUniqueIdentifier().externalId &&
                chan.portId == portIdentifier &&
                chan.id == channelIdentifier &&
                chan.packets.get(sequence) == packet
    }

    override fun verifyPacketAcknowledgement(height: Height, prefix: CommitmentPrefix, proof: CommitmentProof, portIdentifier: Identifier, channelIdentifier: Identifier, sequence: Long, acknowledgement: Acknowledgement): Boolean {
        val hostId = hostIdOf(height) ?: return false
        val notaryKey = notaryKeyOf(height) ?: return false
        val proof = proof as CordaCommitmentProof
        val chan = proof.tx.outputsOfType<Channel>().singleOrNull() ?: return false

        return proof.sig.by == notaryKey &&
                proof.sig.verify(proof.tx.id) &&
                portIdentifier.toUniqueIdentifier().externalId == hostId.toUniqueIdentifier().externalId &&
                channelIdentifier.toUniqueIdentifier().externalId == hostId.toUniqueIdentifier().externalId &&
                chan.portId == portIdentifier &&
                chan.id == channelIdentifier &&
                chan.acknowledgements.get(sequence) == acknowledgement
    }

    override fun verifyPacketAcknowledgementAbsence(height: Height, prefix: CommitmentPrefix, proof: CommitmentProof, portIdentifier: Identifier, channelIdentifier: Identifier, sequence: Long): Boolean {
        throw NotImplementedError()
    }

    override fun verifyNextSequenceRecv(height: Height, prefix: CommitmentPrefix, proof: CommitmentProof, portIdentifier: Identifier, channelIdentifier: Identifier, nextSequenceRecv: Long): Boolean {
        val hostId = hostIdOf(height) ?: return false
        val notaryKey = notaryKeyOf(height) ?: return false
        val proof = proof as CordaCommitmentProof
        val chan = proof.tx.outputsOfType<Channel>().singleOrNull() ?: return false

        return proof.sig.by == notaryKey &&
                proof.sig.verify(proof.tx.id) &&
                portIdentifier.toUniqueIdentifier().externalId == hostId.toUniqueIdentifier().externalId &&
                channelIdentifier.toUniqueIdentifier().externalId == hostId.toUniqueIdentifier().externalId &&
                chan.portId == portIdentifier &&
                chan.id == channelIdentifier &&
                chan.nextSequenceRecv == nextSequenceRecv
    }
}