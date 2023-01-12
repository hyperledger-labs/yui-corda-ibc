package jp.datachain.corda.ibc.grpc_adapter

import ibc.lightclients.corda.v1.CordaTypes
import ibc.lightclients.corda.v1.HostProto
import ibc.lightclients.corda.v1.HostServiceGrpc
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.conversion.toCorda
import jp.datachain.corda.ibc.conversion.toProto
import jp.datachain.corda.ibc.flows.ics24.IbcHostCreateFlow
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria

class HostService(host: String, port: Int, username: String, password: String): HostServiceGrpc.HostServiceImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {

    private fun resolveBaseId(baseId: CordaTypes.StateRef): StateRef {
        return if (baseId == CordaTypes.StateRef.getDefaultInstance()) {
            ops.vaultQuery(Host::class.java).states.single().state.data.baseId
        } else {
            baseId.toCorda()
        }
    }

    override fun createHost(request: HostProto.CreateHostRequest, responseObserver: StreamObserver<HostProto.CreateHostResponse>) {
        val baseId = request.baseId.toCorda() // baseId must be specified when creating a new host
        val moduleNames = request.moduleNamesMap.mapKeys{ Identifier(it.key)}
        val stx = ops.startFlow(::IbcHostCreateFlow, baseId, moduleNames).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = HostProto.CreateHostResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun queryHost(request: HostProto.QueryHostRequest, responseObserver: StreamObserver<HostProto.QueryHostResponse>) {
        val hostAndRef = ops.vaultQueryBy<Host>(
            QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(resolveBaseId(request.baseId).toString())
            )
        ).states.single()
        val host: HostProto.Host = hostAndRef.state.data.toProto()
        val reply = HostProto.QueryHostResponse.newBuilder()
                .setHost(host)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}