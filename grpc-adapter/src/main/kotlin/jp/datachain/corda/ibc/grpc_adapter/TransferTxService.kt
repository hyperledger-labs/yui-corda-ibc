package jp.datachain.corda.ibc.grpc_adapter

import ibc.lightclients.corda.v1.TransferMsgGrpc
import ibc.lightclients.corda.v1.TxTransfer
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.flows.ics20cash.IbcTransferFlow
import jp.datachain.corda.ibc.states.IbcChannel
import net.corda.core.messaging.startFlow

class TransferTxService(host: String, port: Int, username: String, password: String): TransferMsgGrpc.TransferMsgImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun transfer(request: TxTransfer.MsgTransfer, responseObserver: StreamObserver<TxTransfer.MsgTransferResponse>) {
        //ops.startFlow(::IbcSendTransferFlow, baseId, request).returnValue.get()
        val stx = ops.startFlow(::IbcTransferFlow, request.baseId.into(), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val sequence = stx.tx.outputsOfType<IbcChannel>().single().nextSequenceSend - 1
        require(sequence >= 1) // sequences start from 1
        val reply = TxTransfer.MsgTransferResponse.newBuilder()
                .setProof(proof)
                .setSequence(sequence)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}