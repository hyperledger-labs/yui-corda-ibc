package jp.datachain.corda.ibc.grpc_adapter

import ibc.lightclients.corda.v1.Genesis
import ibc.lightclients.corda.v1.GenesisServiceGrpc
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.flows.IbcGenesisCreateFlow
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow

class GenesisService(host: String, port: Int, username: String, password: String): GenesisServiceGrpc.GenesisServiceImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun createGenesis(request: Genesis.CreateGenesisRequest, responseObserver: StreamObserver<Genesis.CreateGenesisResponse>) {
        val stx = ops.startFlow(::IbcGenesisCreateFlow, request.participantsList.map{it.into()}).returnValue.get()
        val baseId = StateRef(txhash = stx.id, index = 0)
        responseObserver.onNext(Genesis.CreateGenesisResponse.newBuilder()
            .setBaseId(baseId.into())
            .build())
        responseObserver.onCompleted()
    }
}