package jp.datachain.corda.ibc.grpc_adapter

import ibc.applications.transfer.v1.MsgGrpc
import ibc.applications.transfer.v1.Tx
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.flows.ics20.IbcSendTransferFlow
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow

class TransferTxService(host: String, port: Int, username: String, password: String, private val baseId: StateRef): MsgGrpc.MsgImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun transfer(request: Tx.MsgTransfer, responseObserver: StreamObserver<Tx.MsgTransferResponse>) {
        ops.startFlow(::IbcSendTransferFlow, baseId, request).returnValue.get()
        responseObserver.onNext(Tx.MsgTransferResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }
}