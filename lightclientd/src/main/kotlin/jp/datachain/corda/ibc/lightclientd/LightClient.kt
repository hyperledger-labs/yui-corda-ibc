package jp.datachain.corda.ibc.lightclientd

import com.google.protobuf.Empty
import ibc.lightclientd.corda.v1.CordaLightclientd
import ibc.lightclientd.corda.v1.LightClientGrpc
import ibc.lightclients.fabric.v1.Fabric
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.CordaClientState
import jp.datachain.corda.ibc.clients.fabric.FabricConsensusState
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics20.toAcknowledgement
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier

class LightClient: LightClientGrpc.LightClientImplBase() {
    private fun withClientState(state: CordaLightclientd.State, f: (cs: CordaClientState) -> Unit) {
        val cs = CordaClientState(emptyList(), state.baseId.into(), state.clientState, state.consensusState)
        f(cs)
    }

    override fun clientType(
        request: CordaLightclientd.ClientTypeRequest,
        responseObserver: StreamObserver<CordaLightclientd.ClientTypeResponse>
    ) = withClientState(request.state) {
        assert(it.clientType() == ClientType.CordaClient)
        responseObserver.onNext(CordaLightclientd.ClientTypeResponse.newBuilder().setClientType("corda").build())
        responseObserver.onCompleted()
    }

    override fun getLatestHeight(
        request: CordaLightclientd.GetLatestHeightRequest,
        responseObserver: StreamObserver<CordaLightclientd.GetLatestHeightResponse>
    ) = withClientState(request.state) {
        responseObserver.onNext(CordaLightclientd.GetLatestHeightResponse.newBuilder().setHeight(it.getLatestHeight()).build())
        responseObserver.onCompleted()
    }

    override fun isFrozen(
        request: CordaLightclientd.IsFrozenRequest,
        responseObserver: StreamObserver<CordaLightclientd.IsFrozenResponse>
    ) = withClientState(request.state) {
        responseObserver.onNext(CordaLightclientd.IsFrozenResponse.newBuilder().setIsFrozen(it.isFrozen()).build())
        responseObserver.onCompleted()
    }

    override fun getFrozenHeight(
        request: CordaLightclientd.GetFrozenHeightRequest,
        responseObserver: StreamObserver<CordaLightclientd.GetFrozenHeightResponse>
    ) = withClientState(request.state) {
        responseObserver.onNext(CordaLightclientd.GetFrozenHeightResponse.newBuilder().setHeight(it.getFrozenHeight()).build())
        responseObserver.onCompleted()
    }

    override fun validate(
        request: CordaLightclientd.ValidateRequest,
        responseObserver: StreamObserver<Empty>
    ) = withClientState(request.state) {
        it.validate()
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun getProofSpecs(
        request: CordaLightclientd.GetProofSpecsRequest,
        responseObserver: StreamObserver<CordaLightclientd.GetProofSpecsResponse>
    ) = withClientState(request.state) {
        responseObserver.onNext(CordaLightclientd.GetProofSpecsResponse.newBuilder().addAllProofSpecs(it.getProofSpecs()).build())
        responseObserver.onCompleted()
    }

    override fun verifyUpgrade(
        request: CordaLightclientd.VerifyUpgradeRequest,
        responseObserver: StreamObserver<Empty>
    ) {
        throw NotImplementedError()
    }

    override fun zeroCustomFields(
        request: CordaLightclientd.ZeroCustomFieldsRequest,
        responseObserver: StreamObserver<CordaLightclientd.ZeroCustomFieldsResponse>
    ) {
        throw NotImplementedError()
    }

    override fun verifyClientState(
        request: CordaLightclientd.VerifyClientStateRequest,
        responseObserver: StreamObserver<Empty>
    ) {
        throw NotImplementedError()
    }

    override fun verifyClientConsensusState(
        request: CordaLightclientd.VerifyClientConsensusStateRequest,
        responseObserver: StreamObserver<Empty>
    ) = withClientState(request.state) {
        // TODO: make this more flexible
        assert(request.consensusState.`is`(Fabric.ConsensusState::class.java))
        it.verifyClientConsensusState(
            request.height,
            Identifier(request.counterpartyClientIdentifier),
            request.consensusHeight,
            request.prefix,
            CommitmentProof(request.proof),
            FabricConsensusState(request.consensusState.unpack(Fabric.ConsensusState::class.java))
        )
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun verifyConnectionState(
        request: CordaLightclientd.VerifyConnectionStateRequest,
        responseObserver: StreamObserver<Empty>
    ) = withClientState(request.state) {
        it.verifyConnectionState(
            request.height,
            request.prefix,
            CommitmentProof(request.proof),
            Identifier(request.connectionId),
            request.connectionEnd
        )
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun verifyChannelState(
        request: CordaLightclientd.VerifyChannelStateRequest,
        responseObserver: StreamObserver<Empty>
    ) = withClientState(request.state) {
        it.verifyChannelState(
            request.height,
            request.prefix,
            CommitmentProof(request.proof),
            Identifier(request.portId),
            Identifier(request.channelId),
            request.channel
        )
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun verifyPacketCommitment(
        request: CordaLightclientd.VerifyPacketCommitmentRequest,
        responseObserver: StreamObserver<Empty>
    ) = withClientState(request.state) {
        it.verifyPacketCommitment(
            request.height,
            request.prefix,
            CommitmentProof(request.proof),
            Identifier(request.portId),
            Identifier(request.channelId),
            request.sequence,
            request.commitmentBytes.toByteArray()
        )
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun verifyPacketAcknowledgement(
        request: CordaLightclientd.VerifyPacketAcknowledgementRequest,
        responseObserver: StreamObserver<Empty>
    ) = withClientState(request.state) {
        it.verifyPacketAcknowledgement(
            request.height,
            request.prefix,
            CommitmentProof(request.proof),
            Identifier(request.portId),
            Identifier(request.channelId),
            request.sequence,
            request.acknowledgement.toAcknowledgement()
        )
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun verifyPacketReceiptAbsence(
        request: CordaLightclientd.VerifyPacketReceiptAbsenceRequest,
        responseObserver: StreamObserver<Empty>
    ) = withClientState(request.state) {
        it.verifyPacketReceiptAbsence(
            request.height,
            request.prefix,
            CommitmentProof(request.proof),
            Identifier(request.portId),
            Identifier(request.channelId),
            request.sequence
        )
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun verifyNextSequenceRecv(
        request: CordaLightclientd.VerifyNextSequenceRecvRequest,
        responseObserver: StreamObserver<Empty>
    ) = withClientState(request.state) {
        it.verifyNextSequenceRecv(
            request.height,
            request.prefix,
            CommitmentProof(request.proof),
            Identifier(request.portId),
            Identifier(request.channelId),
            request.nextSequenceRecv
        )
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }
}