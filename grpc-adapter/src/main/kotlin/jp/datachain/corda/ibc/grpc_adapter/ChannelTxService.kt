package jp.datachain.corda.ibc.grpc_adapter

import ibc.core.channel.v1.MsgGrpc
import ibc.core.channel.v1.Tx
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.flows.ics4.*
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow

class ChannelTxService(host: String, port: Int, username: String, password: String, private val baseId: StateRef): MsgGrpc.MsgImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun channelOpenInit(request: Tx.MsgChannelOpenInit, responseObserver: StreamObserver<Tx.MsgChannelOpenInitResponse>) {
        ops.startFlow(::IbcChanOpenInitFlow, baseId, request).returnValue.get()
        responseObserver.onNext(Tx.MsgChannelOpenInitResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun channelOpenTry(request: Tx.MsgChannelOpenTry, responseObserver: StreamObserver<Tx.MsgChannelOpenTryResponse>) {
        ops.startFlow(::IbcChanOpenTryFlow, baseId, request).returnValue.get()
        responseObserver.onNext(Tx.MsgChannelOpenTryResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun channelOpenAck(request: Tx.MsgChannelOpenAck, responseObserver: StreamObserver<Tx.MsgChannelOpenAckResponse>) {
        ops.startFlow(::IbcChanOpenAckFlow, baseId, request).returnValue.get()
        responseObserver.onNext(Tx.MsgChannelOpenAckResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun channelOpenConfirm(request: Tx.MsgChannelOpenConfirm, responseObserver: StreamObserver<Tx.MsgChannelOpenConfirmResponse>) {
        ops.startFlow(::IbcChanOpenConfirmFlow, baseId, request).returnValue.get()
        responseObserver.onNext(Tx.MsgChannelOpenConfirmResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun channelCloseInit(request: Tx.MsgChannelCloseInit, responseObserver: StreamObserver<Tx.MsgChannelCloseInitResponse>) {
        ops.startFlow(::IbcChanCloseInitFlow, baseId, request).returnValue.get()
        responseObserver.onNext(Tx.MsgChannelCloseInitResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun channelCloseConfirm(request: Tx.MsgChannelCloseConfirm, responseObserver: StreamObserver<Tx.MsgChannelCloseConfirmResponse>) {
        ops.startFlow(::IbcChanCloseConfirmFlow, baseId, request).returnValue.get()
        responseObserver.onNext(Tx.MsgChannelCloseConfirmResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun recvPacket(request: Tx.MsgRecvPacket, responseObserver: StreamObserver<Tx.MsgRecvPacketResponse>) {
        ops.startFlow(::IbcRecvPacketFlow, baseId, request).returnValue.get()
        responseObserver.onNext(Tx.MsgRecvPacketResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun acknowledgement(request: Tx.MsgAcknowledgement, responseObserver: StreamObserver<Tx.MsgAcknowledgementResponse>) {
        ops.startFlow(::IbcAcknowledgePacketFlow, baseId, request).returnValue.get()
        responseObserver.onNext(Tx.MsgAcknowledgementResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }
}