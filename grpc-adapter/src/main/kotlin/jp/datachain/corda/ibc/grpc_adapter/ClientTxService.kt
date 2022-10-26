package jp.datachain.corda.ibc.grpc_adapter

import ibc.lightclients.corda.v1.ClientMsgGrpc
import ibc.lightclients.corda.v1.CordaTypes
import ibc.lightclients.corda.v1.TxClient
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.conversion.toCorda
import jp.datachain.corda.ibc.flows.ics2.IbcClientCreateFlow
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.states.IbcClientState
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow

class ClientTxService(host: String, port: Int, username: String, password: String): ClientMsgGrpc.ClientMsgImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {

    private fun resolveBaseId(baseId: CordaTypes.StateRef): StateRef {
        return if (baseId == CordaTypes.StateRef.getDefaultInstance()) {
            ops.vaultQuery(Host::class.java).states.single().state.data.baseId
        } else {
            baseId.toCorda()
        }
    }

    override fun createClient(request: TxClient.CreateClientRequest, responseObserver: StreamObserver<TxClient.CreateClientResponse>) {
        val stx = ops.startFlow(::IbcClientCreateFlow, resolveBaseId(request.baseId), request.request).returnValue.get()
        val proof = stx.toProof().toByteString()
        val clientId = stx.tx.outputsOfType<IbcClientState>().single().id.id
        val reply = TxClient.CreateClientResponse.newBuilder()
                .setProof(proof)
                .setClientId(clientId)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}