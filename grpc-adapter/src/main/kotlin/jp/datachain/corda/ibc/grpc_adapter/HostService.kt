package jp.datachain.corda.ibc.grpc_adapter

import com.google.protobuf.Empty
import ibc.lightclients.corda.v1.HostProto
import ibc.lightclients.corda.v1.HostServiceGrpc
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.flows.IbcHostCreateFlow
import jp.datachain.corda.ibc.ics24.Host
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria

class HostService(host: String, port: Int, username: String, password: String, private val baseId: StateRef): HostServiceGrpc.HostServiceImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun createHost(request: Empty, responseObserver: StreamObserver<Empty>) {
        ops.startFlow(::IbcHostCreateFlow, baseId).returnValue.get()
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun queryHost(request: Empty, responseObserver: StreamObserver<HostProto.Host>) {
        val hostAndRef = ops.vaultQueryBy<Host>(
            QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString())
            )
        ).states.single()
        val reply: HostProto.Host = hostAndRef.state.data.into()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}