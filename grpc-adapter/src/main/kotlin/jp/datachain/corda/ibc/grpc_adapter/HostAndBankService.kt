package jp.datachain.corda.ibc.grpc_adapter

import com.google.protobuf.Empty
import ibc.lightclients.corda.v1.HostAndBank
import ibc.lightclients.corda.v1.HostAndBankServiceGrpc
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.flows.IbcFundAllocateFlow
import jp.datachain.corda.ibc.flows.IbcHostAndBankCreateFlow
import jp.datachain.corda.ibc.ics20.Address
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics24.Host
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria

class HostAndBankService(host: String, port: Int, username: String, password: String, private val baseId: StateRef): HostAndBankServiceGrpc.HostAndBankServiceImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun createHostAndBank(request: Empty, responseObserver: StreamObserver<Empty>) {
        ops.startFlow(::IbcHostAndBankCreateFlow, baseId).returnValue.get()
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun allocateFund(request: HostAndBank.AllocateFundRequest, responseObserver: StreamObserver<Empty>) {
        ops.startFlow(::IbcFundAllocateFlow,
                baseId,
                Address(request.owner),
                Denom(request.denom),
                Amount(request.amount)
        ).returnValue.get()
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun queryHost(request: Empty, responseObserver: StreamObserver<HostAndBank.Host>) {
        val hostAndRef = ops.vaultQueryBy<Host>(
            QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString())
            )
        ).states.single()
        val reply: HostAndBank.Host = hostAndRef.state.data.into()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun queryBank(request: Empty, responseObserver: StreamObserver<HostAndBank.Bank>) {
        val bankAndRef = ops.vaultQueryBy<Bank>(
            QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString())
            )
        ).states.single()
        val reply: HostAndBank.Bank = bankAndRef.state.data.into()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}