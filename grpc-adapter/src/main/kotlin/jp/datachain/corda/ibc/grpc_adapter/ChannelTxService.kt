package jp.datachain.corda.ibc.grpc_adapter

import ibc.lightclients.corda.v1.ChannelMsgGrpc
import ibc.lightclients.corda.v1.CordaTypes
import ibc.lightclients.corda.v1.TxChannel
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.conversion.toCorda
import jp.datachain.corda.ibc.flows.ics4.*
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.states.IbcChannel
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow

class ChannelTxService(host: String, port: Int, username: String, password: String): ChannelMsgGrpc.ChannelMsgImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {

    private fun resolveBaseId(baseId: CordaTypes.StateRef): StateRef {
        return if (baseId == CordaTypes.StateRef.getDefaultInstance()) {
            ops.vaultQuery(Host::class.java).states.single().state.data.baseId
        } else {
            baseId.toCorda()
        }
    }

    override fun channelOpenInit(request: TxChannel.ChannelOpenInitRequest, responseObserver: StreamObserver<TxChannel.ChannelOpenInitResponse>) {
        val stx = ops.startFlow(::IbcChanOpenInitFlow, resolveBaseId(request.baseId), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val channelId = stx.tx.outputsOfType<IbcChannel>().single().id.id
        val reply = TxChannel.ChannelOpenInitResponse.newBuilder()
                .setProof(proof)
                .setChannelId(channelId)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun channelOpenTry(request: TxChannel.ChannelOpenTryRequest, responseObserver: StreamObserver<TxChannel.ChannelOpenTryResponse>) {
        val stx = ops.startFlow(::IbcChanOpenTryFlow, resolveBaseId(request.baseId), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val channelId = stx.tx.outputsOfType<IbcChannel>().single().id.id
        val reply = TxChannel.ChannelOpenTryResponse.newBuilder()
                .setProof(proof)
                .setChannelId(channelId)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun channelOpenAck(request: TxChannel.ChannelOpenAckRequest, responseObserver: StreamObserver<TxChannel.ChannelOpenAckResponse>) {
        val stx = ops.startFlow(::IbcChanOpenAckFlow, resolveBaseId(request.baseId), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = TxChannel.ChannelOpenAckResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun channelOpenConfirm(request: TxChannel.ChannelOpenConfirmRequest, responseObserver: StreamObserver<TxChannel.ChannelOpenConfirmResponse>) {
        val stx = ops.startFlow(::IbcChanOpenConfirmFlow, resolveBaseId(request.baseId), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = TxChannel.ChannelOpenConfirmResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun channelCloseInit(request: TxChannel.ChannelCloseInitRequest, responseObserver: StreamObserver<TxChannel.ChannelCloseInitResponse>) {
        val stx = ops.startFlow(::IbcChanCloseInitFlow, resolveBaseId(request.baseId), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = TxChannel.ChannelCloseInitResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun channelCloseConfirm(request: TxChannel.ChannelCloseConfirmRequest, responseObserver: StreamObserver<TxChannel.ChannelCloseConfirmResponse>) {
        val stx = ops.startFlow(::IbcChanCloseConfirmFlow, resolveBaseId(request.baseId), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = TxChannel.ChannelCloseConfirmResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun recvPacket(request: TxChannel.RecvPacketRequest, responseObserver: StreamObserver<TxChannel.RecvPacketResponse>) {
        val stx = ops.startFlow(::IbcRecvPacketFlow, resolveBaseId(request.baseId), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = TxChannel.RecvPacketResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun acknowledgement(request: TxChannel.AcknowledgementRequest, responseObserver: StreamObserver<TxChannel.AcknowledgementResponse>) {
        val stx = ops.startFlow(::IbcAcknowledgePacketFlow, resolveBaseId(request.baseId), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = TxChannel.AcknowledgementResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}