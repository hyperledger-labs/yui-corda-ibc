package jp.datachain.corda.ibc.grpc_adapter

import ibc.lightclients.corda.v1.CordaTypes
import ibc.lightclients.corda.v1.TransferMsgGrpc
import ibc.lightclients.corda.v1.TxTransfer
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.conversion.toCorda
import jp.datachain.corda.ibc.flows.ics20cash.IbcTransferFlow
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.states.IbcChannel
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow

class TransferTxService(host: String, port: Int, username: String, password: String): TransferMsgGrpc.TransferMsgImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {

    private fun resolveBaseId(baseId: CordaTypes.StateRef): StateRef {
        return if (baseId == CordaTypes.StateRef.getDefaultInstance()) {
            ops.vaultQuery(Host::class.java).states.single().state.data.baseId
        } else {
            baseId.toCorda()
        }
    }

    override fun transfer(request: TxTransfer.TransferRequest, responseObserver: StreamObserver<TxTransfer.TransferResponse>) {
        //ops.startFlow(::IbcSendTransferFlow, baseId, request).returnValue.get()
        val stx = ops.startFlow(::IbcTransferFlow, resolveBaseId(request.baseId), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val sequence = stx.tx.outputsOfType<IbcChannel>().single().nextSequenceSend - 1
        require(sequence >= 1) // sequences start from 1
        val reply = TxTransfer.TransferResponse.newBuilder()
                .setProof(proof)
                .setSequence(sequence)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}