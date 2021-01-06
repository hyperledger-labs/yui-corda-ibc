package jp.datachain.corda.ibc.grpc_adapter

import ibc.core.connection.v1.MsgGrpc
import ibc.core.connection.v1.Tx
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.flows.*
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort

class ConnectionTxService(host: String, port: Int, username: String, password: String, private val baseId: StateRef): MsgGrpc.MsgImplBase() {
    private val ops = CordaRPCClient(NetworkHostAndPort(host, port))
            .start(username, password)
            .proxy

    override fun connectionOpenInit(request: Tx.MsgConnectionOpenInit, responseObserver: StreamObserver<Tx.MsgConnectionOpenInitResponse>) {
        ops.startFlow(::IbcConnOpenInitFlow, baseId, request).returnValue.get()
        responseObserver.onCompleted()
    }

    override fun connectionOpenTry(request: Tx.MsgConnectionOpenTry, responseObserver: StreamObserver<Tx.MsgConnectionOpenTryResponse>) {
        ops.startFlow(::IbcConnOpenTryFlow, baseId, request).returnValue.get()
        responseObserver.onCompleted()
    }

    override fun connectionOpenAck(request: Tx.MsgConnectionOpenAck, responseObserver: StreamObserver<Tx.MsgConnectionOpenAckResponse>) {
        ops.startFlow(::IbcConnOpenAckFlow, baseId, request).returnValue.get()
        responseObserver.onCompleted()
    }

    override fun connectionOpenConfirm(request: Tx.MsgConnectionOpenConfirm, responseObserver: StreamObserver<Tx.MsgConnectionOpenConfirmResponse>) {
        ops.startFlow(::IbcConnOpenConfirmFlow, baseId, request).returnValue.get()
        responseObserver.onCompleted()
    }
}