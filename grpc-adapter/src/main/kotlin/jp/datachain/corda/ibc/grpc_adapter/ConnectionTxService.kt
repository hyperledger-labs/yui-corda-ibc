package jp.datachain.corda.ibc.grpc_adapter

import ibc.lightclients.corda.v1.ConnectionMsgGrpc
import ibc.lightclients.corda.v1.TxConnection
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.flows.ics3.IbcConnOpenAckFlow
import jp.datachain.corda.ibc.flows.ics3.IbcConnOpenConfirmFlow
import jp.datachain.corda.ibc.flows.ics3.IbcConnOpenInitFlow
import jp.datachain.corda.ibc.flows.ics3.IbcConnOpenTryFlow
import jp.datachain.corda.ibc.states.IbcConnection
import net.corda.core.messaging.startFlow

class ConnectionTxService(host: String, port: Int, username: String, password: String): ConnectionMsgGrpc.ConnectionMsgImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun connectionOpenInit(request: TxConnection.MsgConnectionOpenInit, responseObserver: StreamObserver<TxConnection.MsgConnectionOpenInitResponse>) {
        val stx = ops.startFlow(::IbcConnOpenInitFlow, request.baseId.into(), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val connectionId = stx.tx.outputsOfType<IbcConnection>().single().id.id
        val reply = TxConnection.MsgConnectionOpenInitResponse.newBuilder()
                .setProof(proof)
                .setConnectionId(connectionId)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun connectionOpenTry(request: TxConnection.MsgConnectionOpenTry, responseObserver: StreamObserver<TxConnection.MsgConnectionOpenTryResponse>) {
        val stx = ops.startFlow(::IbcConnOpenTryFlow, request.baseId.into(), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val connectionId = stx.tx.outputsOfType<IbcConnection>().single().id.id
        val reply = TxConnection.MsgConnectionOpenTryResponse.newBuilder()
                .setProof(proof)
                .setConnectionId(connectionId)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun connectionOpenAck(request: TxConnection.MsgConnectionOpenAck, responseObserver: StreamObserver<TxConnection.MsgConnectionOpenAckResponse>) {
        val stx = ops.startFlow(::IbcConnOpenAckFlow, request.baseId.into(), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = TxConnection.MsgConnectionOpenAckResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun connectionOpenConfirm(request: TxConnection.MsgConnectionOpenConfirm, responseObserver: StreamObserver<TxConnection.MsgConnectionOpenConfirmResponse>) {
        val stx = ops.startFlow(::IbcConnOpenConfirmFlow, request.baseId.into(), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = TxConnection.MsgConnectionOpenConfirmResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}