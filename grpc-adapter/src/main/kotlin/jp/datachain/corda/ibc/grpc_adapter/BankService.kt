package jp.datachain.corda.ibc.grpc_adapter

import com.google.protobuf.Empty
import ibc.lightclients.corda.v1.BankProto
import ibc.lightclients.corda.v1.BankServiceGrpc
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.flows.IbcBankCreateFlow
import jp.datachain.corda.ibc.flows.IbcFundAllocateFlow
import jp.datachain.corda.ibc.ics20.Address
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics20.Denom
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria

class BankService(host: String, port: Int, username: String, password: String, private val baseId: StateRef): BankServiceGrpc.BankServiceImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun createBank(request: Empty, responseObserver: StreamObserver<Empty>) {
        ops.startFlow(::IbcBankCreateFlow, baseId).returnValue.get()
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun allocateFund(request: BankProto.AllocateFundRequest, responseObserver: StreamObserver<Empty>) {
        ops.startFlow(::IbcFundAllocateFlow,
                baseId,
                Address(request.owner),
                Denom(request.denom),
                Amount(request.amount)
        ).returnValue.get()
        responseObserver.onNext(Empty.getDefaultInstance())
        responseObserver.onCompleted()
    }

    override fun queryBank(request: Empty, responseObserver: StreamObserver<BankProto.Bank>) {
        val bankAndRef = ops.vaultQueryBy<Bank>(
            QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString())
            )
        ).states.single()
        val reply: BankProto.Bank = bankAndRef.state.data.into()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}