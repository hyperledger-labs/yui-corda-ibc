package jp.datachain.corda.ibc.grpc_adapter

import ibc.lightclients.corda.v1.CashBankProto
import ibc.lightclients.corda.v1.CashBankServiceGrpc
import ibc.lightclients.corda.v1.CordaTypes
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.conversion.toCorda
import jp.datachain.corda.ibc.conversion.toProto
import jp.datachain.corda.ibc.flows.ics20cash.IbcCashBankCreateFlow
import jp.datachain.corda.ibc.ics20.Address
import jp.datachain.corda.ibc.ics20cash.CashBank
import jp.datachain.corda.ibc.ics24.Host
import net.corda.core.contracts.StateRef
import net.corda.core.identity.Party
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.AMOUNT
import net.corda.finance.flows.CashIssueAndPaymentFlow
import java.util.*

class CashBankService(host: String, port: Int, username: String, password: String): CashBankServiceGrpc.CashBankServiceImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {

    private fun resolveBaseId(baseId: CordaTypes.StateRef): StateRef {
        return if (baseId == CordaTypes.StateRef.getDefaultInstance()) {
            ops.vaultQuery(Host::class.java).states.single().state.data.baseId
        } else {
            baseId.toCorda()
        }
    }

    private fun addressToParty(address: String) = ops.partyFromKey(Address.fromBech32(address).toPublicKey())!!

    override fun createCashBank(request: CashBankProto.CreateCashBankRequest, responseObserver: StreamObserver<CashBankProto.CreateCashBankResponse>) {
        val bank = addressToParty(request.bankAddress)
        val stx = ops.startFlow(::IbcCashBankCreateFlow, resolveBaseId(request.baseId), bank).returnValue.get()
        val proof = stx.toProof().toByteString()
        val reply = CashBankProto.CreateCashBankResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun allocateCash(request: CashBankProto.AllocateCashRequest, responseObserver: StreamObserver<CashBankProto.AllocateCashResponse>) {
        val notary = ops.vaultQueryBy<CashBank>(
                QueryCriteria.LinearStateQueryCriteria(
                        externalId = listOf(resolveBaseId(request.baseId).toString())
                )
        ).statesMetadata.single().notary as Party
        val flowRequest = CashIssueAndPaymentFlow.IssueAndPaymentRequest(
                amount = AMOUNT(
                        request.amount.toLong(),
                        Currency.getInstance(request.currency)
                ),
                issueRef = OpaqueBytes(ByteArray(1)),
                recipient = addressToParty(request.ownerAddress),
                notary = notary,
                anonymous = false)
        val stx = ops.startFlow(::CashIssueAndPaymentFlow, flowRequest).returnValue.get().stx
        val proof = stx.toProof().toByteString()
        val reply = CashBankProto.AllocateCashResponse.newBuilder()
                .setProof(proof)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun queryCashBank(request: CashBankProto.QueryCashBankRequest, responseObserver: StreamObserver<CashBankProto.QueryCashBankResponse>) {
        val cashBank: CashBankProto.CashBank = ops.vaultQueryBy<CashBank>(
                QueryCriteria.LinearStateQueryCriteria(
                        externalId = listOf(resolveBaseId(request.baseId).toString())
                )
        ).states.single().state.data.toProto()
        val reply = CashBankProto.QueryCashBankResponse.newBuilder()
                .setCashBank(cashBank)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}