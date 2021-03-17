package jp.datachain.corda.ibc.lightclientd

import com.google.protobuf.Empty
import ibc.lightclientd.corda.v1.CordaLightclientd
import ibc.lightclientd.corda.v1.LightClientGrpc
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.CordaClientState
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics20.toAcknowledgement
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.utilities.NetworkHostAndPort

class LightClient: LightClientGrpc.LightClientImplBase() {
    companion object {
        init {
            warmUpSerialization()
        }

        private fun warmUpSerialization() {
            // port 0 is dummy. This instance of CordaRPCClient is never used.
            // Before using Corda's (de)serialization mechanism, one of four
            // pre-defined serialization environments must be initialized.
            // In the initializer block (init { ... }) of CordaRPCClient,
            // the "node" serialization environment is initialized.
            // Ref: https://github.com/corda/corda/blob/release/os/4.3/client/rpc/src/main/kotlin/net/corda/client/rpc/CordaRPCClient.kt#L435
            CordaRPCClient(NetworkHostAndPort("localhost", 0))
            println("I'm Ready!")
        }
    }

    private fun withClientState(state: CordaLightclientd.State, f: (cs: CordaClientState) -> Unit) {
        val cs = CordaClientState(emptyList(), StateRef(SecureHash.zeroHash, 0), state.clientState, state.consensusState)
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
    ) = withClientState(request.state) {
        it.verifyClientState(
            request.height,
            request.prefix,
            Identifier(request.counterpartyClientIdentifier),
            CommitmentProof(request.proof),
            request.clientState)
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun verifyClientConsensusState(
        request: CordaLightclientd.VerifyClientConsensusStateRequest,
        responseObserver: StreamObserver<Empty>
    ) = withClientState(request.state) {
        it.verifyClientConsensusState(
            request.height,
            Identifier(request.counterpartyClientIdentifier),
            request.consensusHeight,
            request.prefix,
            CommitmentProof(request.proof),
            request.consensusState)
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