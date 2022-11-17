package jp.datachain.corda.ibc.grpc_adapter

import ibc.lightclients.corda.v1.ConnectionMsgGrpc
import ibc.lightclients.corda.v1.CordaTypes
import ibc.lightclients.corda.v1.TxConnection
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.conversion.toCorda
import jp.datachain.corda.ibc.flows.ics3.IbcConnOpenAckFlow
import jp.datachain.corda.ibc.flows.ics3.IbcConnOpenConfirmFlow
import jp.datachain.corda.ibc.flows.ics3.IbcConnOpenInitFlow
import jp.datachain.corda.ibc.flows.ics3.IbcConnOpenTryFlow
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.states.IbcConnection
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow

class ConnectionTxService(host: String, port: Int, username: String, password: String): ConnectionMsgGrpc.ConnectionMsgImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {

    private fun resolveBaseId(baseId: CordaTypes.StateRef): StateRef {
        return if (baseId == CordaTypes.StateRef.getDefaultInstance()) {
            ops.vaultQuery(Host::class.java).states.single().state.data.baseId
        } else {
            baseId.toCorda()
        }
    }

    override fun connectionOpenInit(request: TxConnection.ConnectionOpenInitRequest, responseObserver: StreamObserver<TxConnection.ConnectionOpenInitResponse>) {
        val stx = ops.startFlow(::IbcConnOpenInitFlow, resolveBaseId(request.baseId), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val connectionId = stx.tx.outputsOfType<IbcConnection>().single().id.id
        val reply = TxConnection.ConnectionOpenInitResponse.newBuilder()
                .setProof(proof)
                .setConnectionId(connectionId)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun connectionOpenTry(request: TxConnection.ConnectionOpenTryRequest, responseObserver: StreamObserver<TxConnection.ConnectionOpenTryResponse>) {
        val stx = ops.startFlow(::IbcConnOpenTryFlow, resolveBaseId(request.baseId), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val connectionId = stx.tx.outputsOfType<IbcConnection>().single().id.id
        val reply = TxConnection.ConnectionOpenTryResponse.newBuilder()
                .setProof(proof)
                .setConnectionId(connectionId)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun connectionOpenAck(request: TxConnection.ConnectionOpenAckRequest, responseObserver: StreamObserver<TxConnection.ConnectionOpenAckResponse>) {
        val stx = ops.startFlow(::IbcConnOpenAckFlow, resolveBaseId(request.baseId), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = TxConnection.ConnectionOpenAckResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun connectionOpenConfirm(request: TxConnection.ConnectionOpenConfirmRequest, responseObserver: StreamObserver<TxConnection.ConnectionOpenConfirmResponse>) {
        val stx = ops.startFlow(::IbcConnOpenConfirmFlow, resolveBaseId(request.baseId), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = TxConnection.ConnectionOpenConfirmResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}