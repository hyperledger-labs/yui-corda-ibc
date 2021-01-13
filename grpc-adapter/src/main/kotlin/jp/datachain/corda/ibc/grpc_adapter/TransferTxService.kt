package jp.datachain.corda.ibc.grpc_adapter

import ibc.applications.transfer.v1.MsgGrpc
import ibc.applications.transfer.v1.Tx
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.flows.IbcSendTransferFlow
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort

class TransferTxService(host: String, port: Int, username: String, password: String, private val baseId: StateRef): MsgGrpc.MsgImplBase() {
    private val ops = CordaRPCClient(NetworkHostAndPort(host, port))
            .start(username, password)
            .proxy

    override fun transfer(request: Tx.MsgTransfer, responseObserver: StreamObserver<Tx.MsgTransferResponse>) {
        ops.startFlow(::IbcSendTransferFlow, baseId, request).returnValue.get()
        responseObserver.onNext(Tx.MsgTransferResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }
}