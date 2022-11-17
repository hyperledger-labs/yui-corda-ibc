package jp.datachain.corda.ibc.grpc_adapter

import ibc.lightclients.corda.v1.BankProto
import ibc.lightclients.corda.v1.BankServiceGrpc
import ibc.lightclients.corda.v1.CordaTypes
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.conversion.toCorda
import jp.datachain.corda.ibc.conversion.toProto
import jp.datachain.corda.ibc.flows.ics20.IbcBankCreateFlow
import jp.datachain.corda.ibc.flows.ics20.IbcFundAllocateFlow
import jp.datachain.corda.ibc.ics20.Address
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics24.Host
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria

class BankService(host: String, port: Int, username: String, password: String): BankServiceGrpc.BankServiceImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {

    private fun resolveBaseId(baseId: CordaTypes.StateRef): StateRef {
        return if (baseId == CordaTypes.StateRef.getDefaultInstance()) {
            ops.vaultQuery(Host::class.java).states.single().state.data.baseId
        } else {
            baseId.toCorda()
        }
    }

    override fun createBank(request: BankProto.CreateBankRequest, responseObserver: StreamObserver<BankProto.CreateBankResponse>) {
        val stx = ops.startFlow(::IbcBankCreateFlow, resolveBaseId(request.baseId)).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = BankProto.CreateBankResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun allocateFund(request: BankProto.AllocateFundRequest, responseObserver: StreamObserver<BankProto.AllocateFundResponse>) {
        val stx = ops.startFlow(::IbcFundAllocateFlow,
                resolveBaseId(request.baseId),
                Address.fromBech32(request.owner),
                Denom.fromString(request.denom),
                Amount.fromString(request.amount)
        ).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = BankProto.AllocateFundResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun queryBank(request: BankProto.QueryBankRequest, responseObserver: StreamObserver<BankProto.QueryBankResponse>) {
        val bankAndRef = ops.vaultQueryBy<Bank>(
            QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(resolveBaseId(request.baseId).toString())
            )
        ).states.single()
        val bank: BankProto.Bank = bankAndRef.state.data.toProto()
        val reply = BankProto.QueryBankResponse.newBuilder()
                .setBank(bank)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}