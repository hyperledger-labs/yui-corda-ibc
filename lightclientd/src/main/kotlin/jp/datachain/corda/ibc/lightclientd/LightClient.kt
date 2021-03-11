package jp.datachain.corda.ibc.lightclientd

import com.google.protobuf.Empty
import ibc.lightclientd.corda.v1.Corda
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
    private fun withClientState(state: Corda.State, f: (cs: CordaClientState) -> Unit) {
        val cs = CordaClientState(emptyList(), state.baseId.into(), state.clientState, state.consensusState)
        f(cs)
    }

    override fun clientType(
        request: Corda.ClientTypeRequest,
        responseObserver: StreamObserver<Corda.ClientTypeResponse>
    ) = withClientState(request.state) {
        assert(it.clientType() == ClientType.CordaClient)
        responseObserver.onNext(Corda.ClientTypeResponse.newBuilder().setClientType("corda").build())
        responseObserver.onCompleted()
    }

    override fun getLatestHeight(
        request: Corda.GetLatestHeightRequest,
        responseObserver: StreamObserver<Corda.GetLatestHeightResponse>
    ) = withClientState(request.state) {
        responseObserver.onNext(Corda.GetLatestHeightResponse.newBuilder().setHeight(it.getLatestHeight()).build())
        responseObserver.onCompleted()
    }

    override fun isFrozen(
        request: Corda.IsFrozenRequest,
        responseObserver: StreamObserver<Corda.IsFrozenResponse>
    ) = withClientState(request.state) {
        responseObserver.onNext(Corda.IsFrozenResponse.newBuilder().setIsFrozen(it.isFrozen()).build())
        responseObserver.onCompleted()
    }

    override fun getFrozenHeight(
        request: Corda.GetFrozenHeightRequest,
        responseObserver: StreamObserver<Corda.GetFrozenHeightResponse>
    ) = withClientState(request.state) {
        responseObserver.onNext(Corda.GetFrozenHeightResponse.newBuilder().setHeight(it.getFrozenHeight()).build())
        responseObserver.onCompleted()
    }

    override fun validate(
        request: Corda.ValidateRequest,
        responseObserver: StreamObserver<Empty>
    ) = withClientState(request.state) {
        it.validate()
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun getProofSpecs(
        request: Corda.GetProofSpecsRequest,
        responseObserver: StreamObserver<Corda.GetProofSpecsResponse>
    ) = withClientState(request.state) {
        responseObserver.onNext(Corda.GetProofSpecsResponse.newBuilder().addAllProofSpecs(it.getProofSpecs()).build())
        responseObserver.onCompleted()
    }

    override fun verifyUpgrade(
        request: Corda.VerifyUpgradeRequest,
        responseObserver: StreamObserver<Empty>
    ) {
        throw NotImplementedError()
    }

    override fun zeroCustomFields(
        request: Corda.ZeroCustomFieldsRequest,
        responseObserver: StreamObserver<Corda.ZeroCustomFieldsResponse>
    ) {
        throw NotImplementedError()
    }

    override fun verifyClientState(
        request: Corda.VerifyClientStateRequest,
        responseObserver: StreamObserver<Empty>
    ) {
        throw NotImplementedError()
    }

    override fun verifyClientConsensusState(
        request: Corda.VerifyClientConsensusStateRequest,
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
        request: Corda.VerifyConnectionStateRequest,
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
        request: Corda.VerifyChannelStateRequest,
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
        request: Corda.VerifyPacketCommitmentRequest,
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
        request: Corda.VerifyPacketAcknowledgementRequest,
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
        request: Corda.VerifyPacketReceiptAbsenceRequest,
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
        request: Corda.VerifyNextSequenceRecvRequest,
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