package jp.datachain.corda.ibc.grpc_adapter

import ibc.core.client.v1.MsgGrpc
import ibc.core.client.v1.Tx
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.flows.ics2.IbcClientCreateFlow
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow

class ClientTxService(host: String, port: Int, username: String, password: String, private val baseId: StateRef): MsgGrpc.MsgImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun createClient(request: Tx.MsgCreateClient, responseObserver: StreamObserver<Tx.MsgCreateClientResponse>) {
        ops.startFlow(::IbcClientCreateFlow, baseId, request).returnValue.get()
        responseObserver.onNext(Tx.MsgCreateClientResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }
}