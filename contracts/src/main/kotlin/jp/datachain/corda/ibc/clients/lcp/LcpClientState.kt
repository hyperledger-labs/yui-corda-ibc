package jp.datachain.corda.ibc.clients.lcp

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.r3.conclave.common.OpaqueBytes
import com.r3.conclave.common.internal.SgxReportBody
import com.r3.conclave.common.internal.attestation.EpidAttestation
import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.client.v1.Client
import ibc.core.client.v1.Genesis
import ibc.core.client.v1.compareTo
import ibc.core.commitment.v1.Commitment
import ibc.core.connection.v1.Connection
import ibc.lightclients.lcp.v1.Lcp
import ics23.Proofs
import jp.datachain.corda.ibc.clients.corda.HEIGHT
import jp.datachain.corda.ibc.clients.isZero
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.conversion.pack
import jp.datachain.corda.ibc.conversion.unpack
import jp.datachain.corda.ibc.ics2.*
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.BelongsToContract
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

@BelongsToContract(Ibc::class)
data class LcpClientState constructor(
    override val anyClientState: Any,
    override val anyConsensusStates: Map<Client.Height, Any>
) : ClientState {
    override val consensusStates = anyConsensusStates.mapValues{kv -> LcpConsensusState(kv.value) }
    private val clientState = anyClientState.unpack<Lcp.ClientState>()

    constructor(anyClientState: Any, anyConsensusState: Any) : this(anyClientState, mapOf(HEIGHT to anyConsensusState))

    companion object {
        const val ADDRESS_LENGTH = 20
        const val MRENCLAVE_SIZE = 32

        const val KEY_CLIENT_STORE_PREFIX = "clients"
        const val KEY_CLIENT_STATE = "clientState"
        const val KEY_CONSENSUS_STATE_PREFIX = "consensusStates"
        const val KEY_CONNECTION_PREFIX = "connections"
        const val KEY_CHANNEL_END_PREFIX = "channelEnds"
        const val KEY_CHANNEL_PREFIX = "channels"
        const val KEY_PORT_PREFIX = "ports"
        const val KEY_SEQUENCE_PREFIX = "sequences"
        const val KEY_CHANNEL_CAPABILITY_PREFIX = "capabilities"
        const val KEY_NEXT_SEQ_SEND_PREFIX = "nextSequenceSend"
        const val KEY_NEXT_SEQ_RECV_PREFIX = "nextSequenceRecv"
        const val KEY_NEXT_SEQ_ACK_PREFIX = "nextSequenceAck"
        const val KEY_PACKET_COMMITMENT_PREFIX = "commitments"
        const val KEY_PACKET_ACK_PREFIX = "acks"
        const val KEY_PACKET_RECEIPT_PREFIX = "receipts"
        const val IAS_TRUST_ROOT_CERT =
            """
                -----BEGIN CERTIFICATE-----
                MIIFSzCCA7OgAwIBAgIJANEHdl0yo7CUMA0GCSqGSIb3DQEBCwUAMH4xCzAJBgNV
                BAYTAlVTMQswCQYDVQQIDAJDQTEUMBIGA1UEBwwLU2FudGEgQ2xhcmExGjAYBgNV
                BAoMEUludGVsIENvcnBvcmF0aW9uMTAwLgYDVQQDDCdJbnRlbCBTR1ggQXR0ZXN0
                YXRpb24gUmVwb3J0IFNpZ25pbmcgQ0EwIBcNMTYxMTE0MTUzNzMxWhgPMjA0OTEy
                MzEyMzU5NTlaMH4xCzAJBgNVBAYTAlVTMQswCQYDVQQIDAJDQTEUMBIGA1UEBwwL
                U2FudGEgQ2xhcmExGjAYBgNVBAoMEUludGVsIENvcnBvcmF0aW9uMTAwLgYDVQQD
                DCdJbnRlbCBTR1ggQXR0ZXN0YXRpb24gUmVwb3J0IFNpZ25pbmcgQ0EwggGiMA0G
                CSqGSIb3DQEBAQUAA4IBjwAwggGKAoIBgQCfPGR+tXc8u1EtJzLA10Feu1Wg+p7e
                LmSRmeaCHbkQ1TF3Nwl3RmpqXkeGzNLd69QUnWovYyVSndEMyYc3sHecGgfinEeh
                rgBJSEdsSJ9FpaFdesjsxqzGRa20PYdnnfWcCTvFoulpbFR4VBuXnnVLVzkUvlXT
                L/TAnd8nIZk0zZkFJ7P5LtePvykkar7LcSQO85wtcQe0R1Raf/sQ6wYKaKmFgCGe
                NpEJUmg4ktal4qgIAxk+QHUxQE42sxViN5mqglB0QJdUot/o9a/V/mMeH8KvOAiQ
                byinkNndn+Bgk5sSV5DFgF0DffVqmVMblt5p3jPtImzBIH0QQrXJq39AT8cRwP5H
                afuVeLHcDsRp6hol4P+ZFIhu8mmbI1u0hH3W/0C2BuYXB5PC+5izFFh/nP0lc2Lf
                6rELO9LZdnOhpL1ExFOq9H/B8tPQ84T3Sgb4nAifDabNt/zu6MmCGo5U8lwEFtGM
                RoOaX4AS+909x00lYnmtwsDVWv9vBiJCXRsCAwEAAaOByTCBxjBgBgNVHR8EWTBX
                MFWgU6BRhk9odHRwOi8vdHJ1c3RlZHNlcnZpY2VzLmludGVsLmNvbS9jb250ZW50
                L0NSTC9TR1gvQXR0ZXN0YXRpb25SZXBvcnRTaWduaW5nQ0EuY3JsMB0GA1UdDgQW
                BBR4Q3t2pn680K9+QjfrNXw7hwFRPDAfBgNVHSMEGDAWgBR4Q3t2pn680K9+Qjfr
                NXw7hwFRPDAOBgNVHQ8BAf8EBAMCAQYwEgYDVR0TAQH/BAgwBgEB/wIBADANBgkq
                hkiG9w0BAQsFAAOCAYEAeF8tYMXICvQqeXYQITkV2oLJsp6J4JAqJabHWxYJHGir
                IEqucRiJSSx+HjIJEUVaj8E0QjEud6Y5lNmXlcjqRXaCPOqK0eGRz6hi+ripMtPZ
                sFNaBwLQVV905SDjAzDzNIDnrcnXyB4gcDFCvwDFKKgLRjOB/WAqgscDUoGq5ZVi
                zLUzTqiQPmULAQaB9c6Oti6snEFJiCQ67JLyW/E83/frzCmO5Ru6WjU4tmsmy8Ra
                Ud4APK0wZTGtfPXU7w+IBdG5Ez0kE1qzxGQaL4gINJ1zMyleDnbuS8UicjJijvqA
                152Sq049ESDz+1rRGc2NVEqh1KaGXmtXvqxXcTB+Ljy5Bw2ke0v8iGngFBPqCTVB
                3op5KBG3RjbF6RRSzwzuWfL7QErNC8WEy5yDVARzTA5+xmBc388v9Dm21HGfcC8O
                DD+gT9sSpssq0ascmvH49MOgjt1yoysLtdCtJW/9FZpoOypaHx0R+mJTLwPXVMrv
                DaVzWh5aiEx+idkSGMnX
                -----END CERTIFICATE-----
            """
    }

    override fun clientType(): ClientType = ClientType.LcpClient

    override fun getLatestHeight(): Client.Height = clientState.latestHeight

    override fun validate() {
        require(clientState.keyExpiration != 0L) {
            "KeyExpiration must be non-zero"
        }
        require(clientState.mrenclave.size() == MRENCLAVE_SIZE) {
            "Mrenclave length must be $MRENCLAVE_SIZE, but got ${clientState.mrenclave.size()}"
        }
    }

    override fun getProofSpecs(): List<Proofs.ProofSpec> {
        TODO("Not yet implemented")
    }

    override fun initialize(consState: ConsensusState) {
        validate()
        require(clientState.keysCount == 0 && clientState.attestationTimesCount == 0) {
            "Keys and attestationTimes must be zero: keys = ${clientState.keysCount}, attestationTimes = ${clientState.attestationTimesCount}"
        }
        require(getLatestHeight().isZero()) { "LatestHeight must b zero: height = ${getLatestHeight()}"}
        require(consState is LcpConsensusState) { "Unexpected consensus state type: consensusState = $consState" }
    }

    override fun status(): Status = Status.Active

    override fun exportMetadata(): List<Genesis.GenesisMetadata> {
        TODO("Not yet implemented")
    }

    override fun checkHeaderAndUpdateState(header: Header): Pair<ClientState, ConsensusState> {
        val newStates = when(header) {
            is LcpUpdateClientHeader -> checkHeaderAndUpdateForUpdateClient(header)
            is LcpRegisterEnclaveKeyHeader -> checkHeaderAndUpdateForRegisterEnclaveKey(header)
            else -> throw IllegalArgumentException("Invalid header: $header")
        }
        return newStates
    }

    private fun checkHeaderAndUpdateForUpdateClient(header: LcpUpdateClientHeader): Pair<ClientState, ConsensusState> {
        val commitment = UpdateClientCommitment.decodeRlp(header.header.commitment)
        require(commitment != null) { "Failed to decode commitment: commitment = ${header.header.commitment}" }

        if (getLatestHeight().isZero()) {
            require(commitment!!.newState.isNotEmpty()) { "The commitment must have newState: commitment = $commitment" }
        } else {
            require(!commitment!!.prevHeight.isZero() && commitment.prevStateId.isNotEmpty()) {
                "The commitment must have prevHeight and prevStateId: commitment = $commitment"
            }
            val prevConsensusState = Lcp.ConsensusState.newBuilder().build()
            require(prevConsensusState.stateId == ByteString.copyFrom(commitment.prevStateId)) {
                "Unmatched stateId: prevConsensusState.stateId != commitment.prevStateId (${prevConsensusState.stateId} != ${commitment.prevStateId})"
            }
        }

        require(isActiveKey(header.header.signer)) { "Invalid signer: signer = ${header.header.signer}" }

        verifySignature(header.header.commitment.toByteArray(), header.header.signature.toByteArray(), header.header.signer.toStringUtf8())

        val newLcpClient = if (header.getHeight() > this.clientState.latestHeight) {
            this.clientState.toBuilder()
                .setLatestHeight(header.getHeight())
                .build()
        } else {
            this.clientState
        }

        val newConsensusState = Lcp.ConsensusState.newBuilder()
            .setStateId(ByteString.copyFrom(commitment.newStateId))
            .setTimestamp(commitment.timestamp)
            .build()

        val newClientState = this.copy(
            anyClientState = newLcpClient.pack(),
            anyConsensusStates = this.anyConsensusStates + Pair(header.getHeight(), newConsensusState.pack())
        )

        return Pair(newClientState, LcpConsensusState(newConsensusState.pack()))
    }

    private fun checkHeaderAndUpdateForRegisterEnclaveKey(header: LcpRegisterEnclaveKeyHeader): Pair<ClientState, ConsensusState> {
        val factory = CertificateFactory.getInstance("X.509")
        val inputStream = ByteArrayInputStream(header.header.signingCert.toByteArray())
        val cert = factory.generateCertificate(inputStream) as X509Certificate
        val rootInputStream = ByteArrayInputStream(IAS_TRUST_ROOT_CERT.toByteArray())
        val iasRootCert = factory.generateCertificate(rootInputStream) as X509Certificate
        val certPath = factory.generateCertPath(listOf(cert, iasRootCert))

        // EpidAttestation verifies Report before instantiating.
        val attestation = EpidAttestation(OpaqueBytes.parse(header.header.report), OpaqueBytes.of(*header.header.signature.toByteArray()), certPath)
        // TODO: Check a securitySummary and advisoryIDs

        val mrenclave = ByteString.copyFrom(attestation.reportBody[SgxReportBody.mrenclave].read())
        require(clientState.mrenclave == mrenclave) { "Unmatched mrenclave: clientState.mrenclave != mrenclve (${clientState.mrenclave} != $mrenclave)" }

        val signer = ByteString.copyFrom(attestation.reportBody[SgxReportBody.reportData].read().array().take(ADDRESS_LENGTH).toByteArray())
        require(!clientState.keysList.contains(signer)) { "Signer already exists: signer = $signer" }

        val newClientState = clientState.toBuilder()
            .addKeys(signer)
            .build()

        return Pair(this.copy(anyClientState = newClientState.pack()), this.consensusStates[getLatestHeight()]!!)
    }

    override fun checkMisbehaviourAndUpdateState(misbehaviour: Misbehaviour): ClientState {
        TODO("Not yet implemented")
    }

    override fun checkSubstituteAndUpdateState(substituteClient: ClientState): ClientState {
        TODO("Not yet implemented")
    }

    override fun verifyUpgradeAndUpdateState(
        newClient: ClientState,
        newConsState: ConsensusState,
        proofUpgradeClient: CommitmentProof,
        proofUpgradeConsState: CommitmentProof
    ): Pair<ClientState, ConsensusState> {
        TODO("Not yet implemented")
    }

    override fun zeroCustomFields(): ClientState {
        TODO("Not yet implemented")
    }

    override fun verifyClientState(
        height: Client.Height,
        prefix: Commitment.MerklePrefix,
        counterpartyClientIdentifier: Identifier,
        proof: CommitmentProof,
        clientState: Any
    ) {
        val clientsPath = clientPath(counterpartyClientIdentifier)
        val fullPath = Commitment.MerklePath.newBuilder()
            .addKeyPathBytes(prefix.toByteString())
            .addKeyPath(clientsPath)
            .build()
        verifyMembership(height, proof, fullPath, clientState.toByteArray())
    }

    private fun clientPath(id: Identifier): String {
        return "$KEY_CLIENT_STORE_PREFIX/${id.id}/$KEY_CLIENT_STATE"
    }

    override fun verifyClientConsensusState(
        height: Client.Height,
        counterpartyClientIdentifier: Identifier,
        consensusHeight: Client.Height,
        prefix: Commitment.MerklePrefix,
        proof: CommitmentProof,
        consensusState: Any
    ) {
        TODO("Not yet implemented")
    }

    override fun verifyConnectionState(
        height: Client.Height,
        prefix: Commitment.MerklePrefix,
        proof: CommitmentProof,
        connectionID: Identifier,
        connectionEnd: Connection.ConnectionEnd
    ) {
        val connectionPath = connectionId(connectionID)
        val fullPath = Commitment.MerklePath.newBuilder()
            .addKeyPathBytes(prefix.toByteString())
            .addKeyPath(connectionPath)
            .build()
        verifyMembership(height, proof, fullPath, connectionEnd.toByteArray())
    }

    private fun connectionId(id: Identifier): String {
        return "$KEY_CONNECTION_PREFIX/${id.id}"
    }

    override fun verifyChannelState(
        height: Client.Height,
        prefix: Commitment.MerklePrefix,
        proof: CommitmentProof,
        portID: Identifier,
        channelID: Identifier,
        channel: ChannelOuterClass.Channel
    ) {
        val channelPath = channelEndPath(portID, channelID)
        val fullPath = Commitment.MerklePath.newBuilder()
            .addKeyPathBytes(prefix.toByteString())
            .addKeyPath(channelPath)
            .build()
        verifyMembership(height, proof, fullPath, channel.toByteArray())
    }

    private fun channelEndPath(portId: Identifier, channelId: Identifier): String {
        return "$KEY_CHANNEL_END_PREFIX/${channelPath(portId, channelId)}"
    }

    private fun channelPath(portId: Identifier, channelId: Identifier): String {
        return "$KEY_PORT_PREFIX/${portId.id}/$KEY_CHANNEL_PREFIX/${channelId.id}"
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
        val packetCommitmentPath = packetCommitmentPath(portID, channelID, sequence)
        val fullPath = Commitment.MerklePath.newBuilder()
            .addKeyPathBytes(prefix.toByteString())
            .addKeyPath(packetCommitmentPath)
            .build()
        verifyMembership(height, proof, fullPath, commitmentBytes)
    }

    private fun packetCommitmentPath(portId: Identifier, channelId: Identifier, sequence: Long): String {
        return "$KEY_PACKET_COMMITMENT_PREFIX/${channelPath(portId, channelId)}/$KEY_SEQUENCE_PREFIX/$sequence"
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
        val packetAcknowledgementPath = packetAcknowledgementPath(portID, channelID, sequence)
        val fullPath = Commitment.MerklePath.newBuilder()
            .addKeyPathBytes(prefix.toByteString())
            .addKeyPath(packetAcknowledgementPath)
            .build()
        verifyMembership(height, proof, fullPath, acknowledgement.toByteArray())
    }

    private fun packetAcknowledgementPath(portId: Identifier, channelId: Identifier, sequence: Long): String {
        return "$KEY_PACKET_ACK_PREFIX/${channelPath(portId, channelId)}/$KEY_SEQUENCE_PREFIX/$sequence"
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
        val packetAcknowledgementPath = packetReceiptPath(portID, channelID, sequence)
        val fullPath = Commitment.MerklePath.newBuilder()
            .addKeyPathBytes(prefix.toByteString())
            .addKeyPath(packetAcknowledgementPath)
            .build()
        verifyMembership(height, proof, fullPath, fullPath.toByteArray())
    }

    private fun packetReceiptPath(portId: Identifier, channelId: Identifier, sequence: Long): String {
        return "$KEY_PACKET_RECEIPT_PREFIX/${channelPath(portId, channelId)}/$KEY_SEQUENCE_PREFIX/$sequence"
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
        val packetAcknowledgementPath = nextSequenceRecvPath(portID, channelID)
        val fullPath = Commitment.MerklePath.newBuilder()
            .addKeyPathBytes(prefix.toByteString())
            .addKeyPath(packetAcknowledgementPath)
            .build()
        // NOTE: Long.SIZE_BYTES will be available since kotlin 1.3.
        val buf = ByteBuffer.allocate(8).putLong(nextSequenceRecv)
        verifyMembership(height, proof, fullPath, buf.array())
    }

    private fun nextSequenceRecvPath(portId: Identifier, channelId: Identifier): String {
        return "$KEY_NEXT_SEQ_RECV_PREFIX/${channelPath(portId, channelId)}"
    }

    private fun isActiveKey(signer: ByteString): Boolean {
        for ((key, time) in this.clientState.keysList.zip(this.clientState.attestationTimesList)) {
            // TODO: Current time should be block time, not system time.
            val current = System.currentTimeMillis() / 1000
            val expired = current - this.clientState.keyExpiration
            return key == signer && time > expired
        }
        return false
    }

    private fun verifySignature(message: ByteArray, signature: ByteArray, signer: String) {
        val sd = Sign.SignatureData(
            signature[64],
            signature.copyOfRange(0, 32),
            signature.copyOfRange(32, 64)
        )
        val pubKey = Sign.signedMessageToKey(message, sd)
        val address = Keys.getAddress(pubKey)
        require(address == signer) { "Signers must be equal. address != signer ($address != $signer)"}
        return
    }

    private fun verifyMembership(
        height: Client.Height,
        proof: CommitmentProof,
        path: Commitment.MerklePath,
        value: ByteArray
    ) {
        require(path.keyPathCount == 2) { "Invalid key path length" }
        val prefix: Commitment.MerklePrefix = Commitment.MerklePrefix.newBuilder()
            .setKeyPrefix(ByteString.copyFromUtf8(path.getKeyPath(0)))
            .build()
        val commitmentPath = path.getKeyPath(1)
        val commitmentValue = Hash.sha3(value)
        require(getLatestHeight() >= height) { "ClientState must have sufficient height. getLatestHeight() >= height (${getLatestHeight()} >= $height)" }
        val consensusState = consensusStates[height]
        require(consensusState != null) { "ConsensusState doesn't exist: height = $height" }

        val commitmentProof = StateCommitmentProof.decodeRlp(proof.toByteString())
        require(commitmentProof != null) { "Failed to decode commitmentProof: proof = $proof" }
        val commitment = StateCommitment.rlpDecode(ByteString.copyFrom(commitmentProof!!.commitmentBytes))
        require(commitment != null) {
            "Failed to decode commitment: commitment = ${commitmentProof.commitmentBytes}"
        }
        require(commitment!!.height == height) {
            "Unmatched height: commitment.height != height (${commitment.height} != ${height})"
        }
        require(commitment.prefix == prefix) {
            "Unmatched prefix: commitment.prefix != prefix (${commitment.prefix} != ${prefix})"
        }
        require(commitment.path == commitmentPath) {
            "Unmatched path: commitment.path != commitmentPath (${commitment.path} != ${commitmentPath})"
        }
        require(commitment.value.contentEquals(commitmentValue)) {
            "Unmatched value: commitment.value != commitmentValue (${commitment.value} != ${commitmentValue})"
        }
        val stateId = consensusState!!.consensusState.stateId.toByteArray()
        require(commitment.stateId.contentEquals(stateId)) {
            "Unmatched stateId: commitment.stateId != stateId (${commitment.stateId} != $stateId)"
        }

        verifySignature(commitmentProof.commitmentBytes, commitmentProof.signature, commitmentProof.signer)

        require(isActiveKey(ByteString.copyFromUtf8(commitmentProof.signer))) {
            "Invalid signer: signer = ${commitmentProof.signer}"
        }
    }
}
