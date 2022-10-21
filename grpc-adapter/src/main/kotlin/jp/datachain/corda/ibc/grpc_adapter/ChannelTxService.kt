package jp.datachain.corda.ibc.grpc_adapter

import ibc.lightclients.corda.v1.ChannelMsgGrpc
import ibc.lightclients.corda.v1.TxChannel
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.flows.ics4.*
import jp.datachain.corda.ibc.states.IbcChannel
import net.corda.core.messaging.startFlow

class ChannelTxService(host: String, port: Int, username: String, password: String): ChannelMsgGrpc.ChannelMsgImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun channelOpenInit(request: TxChannel.MsgChannelOpenInit, responseObserver: StreamObserver<TxChannel.MsgChannelOpenInitResponse>) {
        val stx = ops.startFlow(::IbcChanOpenInitFlow, request.baseId.into(), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val channelId = stx.tx.outputsOfType<IbcChannel>().single().id.id
        val reply = TxChannel.MsgChannelOpenInitResponse.newBuilder()
                .setProof(proof)
                .setChannelId(channelId)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun channelOpenTry(request: TxChannel.MsgChannelOpenTry, responseObserver: StreamObserver<TxChannel.MsgChannelOpenTryResponse>) {
        val stx = ops.startFlow(::IbcChanOpenTryFlow, request.baseId.into(), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val channelId = stx.tx.outputsOfType<IbcChannel>().single().id.id
        val reply = TxChannel.MsgChannelOpenTryResponse.newBuilder()
                .setProof(proof)
                .setChannelId(channelId)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun channelOpenAck(request: TxChannel.MsgChannelOpenAck, responseObserver: StreamObserver<TxChannel.MsgChannelOpenAckResponse>) {
        val stx = ops.startFlow(::IbcChanOpenAckFlow, request.baseId.into(), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = TxChannel.MsgChannelOpenAckResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun channelOpenConfirm(request: TxChannel.MsgChannelOpenConfirm, responseObserver: StreamObserver<TxChannel.MsgChannelOpenConfirmResponse>) {
        val stx = ops.startFlow(::IbcChanOpenConfirmFlow, request.baseId.into(), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = TxChannel.MsgChannelOpenConfirmResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun channelCloseInit(request: TxChannel.MsgChannelCloseInit, responseObserver: StreamObserver<TxChannel.MsgChannelCloseInitResponse>) {
        val stx = ops.startFlow(::IbcChanCloseInitFlow, request.baseId.into(), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = TxChannel.MsgChannelCloseInitResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun channelCloseConfirm(request: TxChannel.MsgChannelCloseConfirm, responseObserver: StreamObserver<TxChannel.MsgChannelCloseConfirmResponse>) {
        val stx = ops.startFlow(::IbcChanCloseConfirmFlow, request.baseId.into(), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = TxChannel.MsgChannelCloseConfirmResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun recvPacket(request: TxChannel.MsgRecvPacket, responseObserver: StreamObserver<TxChannel.MsgRecvPacketResponse>) {
        val stx = ops.startFlow(::IbcRecvPacketFlow, request.baseId.into(), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = TxChannel.MsgRecvPacketResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun acknowledgement(request: TxChannel.MsgAcknowledgement, responseObserver: StreamObserver<TxChannel.MsgAcknowledgementResponse>) {
        val stx = ops.startFlow(::IbcAcknowledgePacketFlow, request.baseId.into(), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = TxChannel.MsgAcknowledgementResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}