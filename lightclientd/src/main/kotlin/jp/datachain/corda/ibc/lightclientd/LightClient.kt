package jp.datachain.corda.ibc.lightclientd

import com.google.protobuf.Empty
import ibc.lightclientd.corda.v1.Lightclientd
import ibc.lightclientd.corda.v1.LightClientGrpc
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.CordaClientState
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

    private fun withClientState(state: Lightclientd.State, f: (cs: CordaClientState) -> Unit) {
        val cs = CordaClientState(emptyList(), StateRef(SecureHash.zeroHash, 0), state.clientState, state.consensusState)
        f(cs)
    }

    override fun verifyClientState(
        request: Lightclientd.VerifyClientStateRequest,
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
        request: Lightclientd.VerifyClientConsensusStateRequest,
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
        request: Lightclientd.VerifyConnectionStateRequest,
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
        request: Lightclientd.VerifyChannelStateRequest,
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
        request: Lightclientd.VerifyPacketCommitmentRequest,
        responseObserver: StreamObserver<Empty>
    ) = withClientState(request.state) {
        it.verifyPacketCommitment(
            request.height,
            request.delayTimePeriod,
            request.delayBlockPeriod,
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
        request: Lightclientd.VerifyPacketAcknowledgementRequest,
        responseObserver: StreamObserver<Empty>
    ) = withClientState(request.state) {
        it.verifyPacketAcknowledgement(
            request.height,
            request.delayTimePeriod,
            request.delayBlockPeriod,
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
        request: Lightclientd.VerifyPacketReceiptAbsenceRequest,
        responseObserver: StreamObserver<Empty>
    ) = withClientState(request.state) {
        it.verifyPacketReceiptAbsence(
            request.height,
            request.delayTimePeriod,
            request.delayBlockPeriod,
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
        request: Lightclientd.VerifyNextSequenceRecvRequest,
        responseObserver: StreamObserver<Empty>
    ) = withClientState(request.state) {
        it.verifyNextSequenceRecv(
            request.height,
            request.delayTimePeriod,
            request.delayBlockPeriod,
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