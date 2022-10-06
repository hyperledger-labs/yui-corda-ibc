package jp.datachain.corda.ibc.clients.fabric

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.client.v1.Client
import ibc.core.client.v1.Genesis
import ibc.core.commitment.v1.Commitment
import ibc.core.connection.v1.Connection
import ibc.lightclientd.fabric.v1.LightClientGrpc
import ibc.lightclientd.fabric.v1.Lightclientd
import ibc.lightclients.fabric.v1.Fabric
import ics23.Proofs
import io.grpc.ManagedChannelBuilder
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics2.*
import jp.datachain.corda.ibc.ics20.toJson
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.StateRef
import net.corda.core.identity.AbstractParty

@BelongsToContract(Ibc::class)
data class FabricClientState constructor(
    override val participants: List<AbstractParty>,
    override val baseId: StateRef,
    val fabricClientState: Fabric.ClientState,
    val fabricConsensusStates: Map<Long, Fabric.ConsensusState>
) : ClientState {
    override val id get() = Identifier(fabricClientState.id)
    override val clientState get() = Any.pack(fabricClientState, "")!!
    override val consensusStates get() = fabricConsensusStates
        .mapKeys { e ->
            Client.Height.newBuilder()
                .setRevisionNumber(0)
                .setRevisionHeight(e.key)
                .build()
        }
        .mapValues { e ->
            FabricConsensusState(e.value)
        }

    constructor(host: Host, id: Identifier, fabricClientState: Fabric.ClientState, fabricConsensusState: Fabric.ConsensusState) : this(
        host.participants,
        host.baseId,
        fabricClientState,
        mapOf(fabricClientState.lastChaincodeHeader.sequence.value to fabricConsensusState)
    ) {
        require(id.id == fabricClientState.id)
    }

    private fun <R> withLightClientStub(f: (lc: LightClientGrpc.LightClientBlockingStub) -> R): R {
        val channel = ManagedChannelBuilder
            .forTarget("localhost:60001")
            .usePlaintext()
            .build()
        try {
            val lc = LightClientGrpc.newBlockingStub(channel)
            return f(lc)
        } finally {
            channel.shutdown()
        }
    }
    private fun makeState() = Lightclientd.State.newBuilder()
        .setClientState(fabricClientState)
        .also { builder ->
            consensusStates.forEach{ (height, consensusState) ->
                builder.putConsensusStates(height.revisionHeight, consensusState.fabricConsensusState)
            }
        }
        .build()

    override fun clientType() = withLightClientStub {
        val req = Lightclientd.ClientTypeRequest
            .newBuilder()
            .setState(makeState())
            .build()
        val res = it.clientType(req)
        require(res.clientType == "fabric")
        ClientType.FabricClient
    }
    override fun getLatestHeight(): Client.Height = withLightClientStub {
        val req = Lightclientd.GetLatestHeightRequest
            .newBuilder()
            .setState(makeState())
            .build()
        val res = it.getLatestHeight(req)
        res.height
    }
    override fun validate() = withLightClientStub {
        val req = Lightclientd.ValidateRequest
            .newBuilder()
            .setState(makeState())
            .build()
        it.validate(req)
        Unit
    }
    override fun getProofSpecs(): List<Proofs.ProofSpec> = withLightClientStub {
        val req = Lightclientd.GetProofSpecsRequest
            .newBuilder()
            .setState(makeState())
            .build()
        val res = it.getProofSpecs(req)
        res.proofSpecsList
    }

    override fun initialize(consState: ConsensusState) = withLightClientStub {
        val req = Lightclientd.InitializeRequest
                .newBuilder()
                .setState(makeState())
                .setConsensusState((consState as FabricConsensusState).fabricConsensusState)
                .build()
        it.initialize(req)
        Unit
    }

    override fun status() = withLightClientStub {
        val req = Lightclientd.StatusRequest
                .newBuilder()
                .setState(makeState())
                .build()
        val res = it.status(req)
        Status.valueOf(res.status)
    }

    override fun exportMetadata(): List<Genesis.GenesisMetadata> = withLightClientStub {
        val req = Lightclientd.ExportMetadataRequest
                .newBuilder()
                .setState(makeState())
                .build()
        val res = it.exportMetadata(req)
        res.genesisMetadatasList
    }

    override fun checkHeaderAndUpdateState(header: Header): Pair<ClientState, ConsensusState> {
        return withLightClientStub {
            val req = Lightclientd.CheckHeaderAndUpdateStateRequest
                .newBuilder()
                .setState(makeState())
                .setHeader((header as FabricHeader).fabricHeader)
                .build()
            val res = it.checkHeaderAndUpdateState(req)
            val newClientState = this.copy(
                fabricClientState = res.state.clientState,
                fabricConsensusStates = res.state.consensusStatesMap
            )
            val newConsensusState = newClientState.consensusStates[newClientState.getLatestHeight()]!!
            Pair(newClientState, newConsensusState)
        }
    }
    override fun checkMisbehaviourAndUpdateState(misbehaviour: Misbehaviour) = throw NotImplementedError()
    override fun checkSubstituteAndUpdateState(substituteClient: ClientState) = throw NotImplementedError()

    override fun verifyUpgradeAndUpdateState(newClient: ClientState, newConsState: ConsensusState, proofUpgradeClient: CommitmentProof, proofUpgradeConsState: CommitmentProof): Pair<ClientState, ConsensusState> = withLightClientStub {
        val req = Lightclientd.VerifyUpgradeAndUpdateStateRequest
                .newBuilder()
                .setState(makeState())
                .setNewClient((newClient as FabricClientState).fabricClientState)
                .setNewConsState((newConsState as FabricConsensusState).fabricConsensusState)
                .setProofUpgradeClient(ByteString.copyFrom(proofUpgradeClient.bytes))
                .setProofUpgradeConsState(ByteString.copyFrom(proofUpgradeConsState.bytes))
                .build()
        val res = it.verifyUpgradeAndUpdateState(req)
        val newClientState = this.copy(
                fabricClientState = res.state.clientState,
                fabricConsensusStates = res.state.consensusStatesMap
        )
        val newConsensusState = newClientState.consensusStates[newClientState.getLatestHeight()]!!
        Pair(newClientState, newConsensusState)
    }

    override fun zeroCustomFields(): ClientState = withLightClientStub {
        TODO("This can't implemented until ClientState is divided into a part independent of Corda and a Corda state")
    }

    override fun verifyClientState(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            counterpartyClientIdentifier: Identifier,
            proof: CommitmentProof,
            clientState: Any
    ) = withLightClientStub {
        val req = Lightclientd.VerifyClientStateRequest
            .newBuilder()
            .setState(makeState())
            .setHeight(height)
            .setPrefix(prefix)
            .setCounterpartyClientIdentifier(counterpartyClientIdentifier.id)
            .setProof(proof.toByteString())
            .setClientState(clientState)
            .build()
        it.verifyClientState(req)
        Unit
    }

    override fun verifyClientConsensusState(
            height: Client.Height,
            counterpartyClientIdentifier: Identifier,
            consensusHeight: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            consensusState: Any
    ) = withLightClientStub {
        val req = Lightclientd.VerifyClientConsensusStateRequest
            .newBuilder()
            .setState(makeState())
            .setHeight(height)
            .setCounterpartyClientIdentifier(counterpartyClientIdentifier.id)
            .setConsensusHeight(consensusHeight)
            .setPrefix(prefix)
            .setProof(proof.toByteString())
            .setConsensusState(consensusState)
            .build()
        it.verifyClientConsensusState(req)
        Unit
    }

    override fun verifyConnectionState(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            connectionID: Identifier,
            connectionEnd: Connection.ConnectionEnd
    ) = withLightClientStub {
        val req = Lightclientd.VerifyConnectionStateRequest
            .newBuilder()
            .setState(makeState())
            .setHeight(height)
            .setPrefix(prefix)
            .setProof(proof.toByteString())
            .setConnectionId(connectionID.id)
            .setConnectionEnd(connectionEnd)
            .build()
        it.verifyConnectionState(req)
        Unit
    }

    override fun verifyChannelState(
            height: Client.Height,
            prefix: Commitment.MerklePrefix,
            proof: CommitmentProof,
            portID: Identifier,
            channelID: Identifier,
            channel: ChannelOuterClass.Channel
    ) = withLightClientStub {
        val req = Lightclientd.VerifyChannelStateRequest
            .newBuilder()
            .setState(makeState())
            .setHeight(height)
            .setPrefix(prefix)
            .setProof(proof.toByteString())
            .setPortId(portID.id)
            .setChannelId(channelID.id)
            .setChannel(channel)
            .build()
        it.verifyChannelState(req)
        Unit
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
    ) = withLightClientStub {
        val req = Lightclientd.VerifyPacketCommitmentRequest
            .newBuilder()
            .setState(makeState())
            .setHeight(height)
            .setDelayTimePeriod(delayTimePeriod)
            .setDelayBlockPeriod(delayBlockPeriod)
            .setPrefix(prefix)
            .setProof(proof.toByteString())
            .setPortId(portID.id)
            .setChannelId(channelID.id)
            .setSequence(sequence)
            .setCommitmentBytes(ByteString.copyFrom(commitmentBytes))
            .build()
        it.verifyPacketCommitment(req)
        Unit
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
    ) = withLightClientStub {
        val req = Lightclientd.VerifyPacketAcknowledgementRequest
            .newBuilder()
            .setState(makeState())
            .setHeight(height)
            .setDelayTimePeriod(delayTimePeriod)
            .setDelayBlockPeriod(delayBlockPeriod)
            .setPrefix(prefix)
            .setProof(proof.toByteString())
            .setPortId(portID.id)
            .setChannelId(channelID.id)
            .setSequence(sequence)
            .setAcknowledgement(acknowledgement.toJson())
            .build()
        it.verifyPacketAcknowledgement(req)
        Unit
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
    ) = withLightClientStub {
        val req = Lightclientd.VerifyPacketReceiptAbsenceRequest
            .newBuilder()
            .setState(makeState())
            .setHeight(height)
            .setDelayTimePeriod(delayTimePeriod)
            .setDelayBlockPeriod(delayBlockPeriod)
            .setPrefix(prefix)
            .setProof(proof.toByteString())
            .setPortId(portID.id)
            .setChannelId(channelID.id)
            .setSequence(sequence)
            .build()
        it.verifyPacketReceiptAbsence(req)
        Unit
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
    ) = withLightClientStub {
        val req = Lightclientd.VerifyNextSequenceRecvRequest
            .newBuilder()
            .setState(makeState())
            .setHeight(height)
            .setDelayTimePeriod(delayTimePeriod)
            .setDelayBlockPeriod(delayBlockPeriod)
            .setPrefix(prefix)
            .setProof(proof.toByteString())
            .setPortId(portID.id)
            .setChannelId(channelID.id)
            .setNextSequenceRecv(nextSequenceRecv)
            .build()
        it.verifyNextSequenceRecv(req)
        Unit
    }
}