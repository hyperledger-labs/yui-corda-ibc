package jp.datachain.corda.ibc.grpc_adapter

import ibc.lightclients.corda.v1.Query
import ibc.lightclients.corda.v1.QueryServiceGrpc
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics24.Host
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria

class HostAndBankQueryService(host: String, port: Int, username: String, password: String, private val baseId: StateRef): QueryServiceGrpc.QueryServiceImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun queryHost(request: Query.QueryHostRequest, responseObserver: StreamObserver<Query.Host>) {
        val hostAndRef = ops.vaultQueryBy<Host>(
                QueryCriteria.LinearStateQueryCriteria(
                        externalId = listOf(baseId.toString())
                )
        ).states.single()
        val reply: Query.Host = hostAndRef.state.data.into()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun queryBank(request: Query.QueryBankRequest, responseObserver: StreamObserver<Query.Bank>) {
        val bankAndRef = ops.vaultQueryBy<Bank>(
                QueryCriteria.LinearStateQueryCriteria(
                        externalId = listOf(baseId.toString())
                )
        ).states.single()
        val reply: Query.Bank = bankAndRef.state.data.into()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}