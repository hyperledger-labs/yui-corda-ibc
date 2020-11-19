package jp.datachain.corda.ibc.grpc

import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.flows.IbcClientCreateFlow
import jp.datachain.corda.ibc.flows.IbcFundAllocateFlow
import jp.datachain.corda.ibc.flows.IbcGenesisCreateFlow
import jp.datachain.corda.ibc.flows.IbcHostAndBankCreateFlow
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics20.Denom
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.NetworkHostAndPort
import jp.datachain.corda.ibc.ics24.Host
import net.corda.core.messaging.startFlow
import jp.datachain.corda.ibc.grpc.Host as GrpcHost
import jp.datachain.corda.ibc.grpc.Bank as GrpcBank
import jp.datachain.corda.ibc.grpc.SignedTransaction as GrpcSignedTransaction

class GrpcIbcService(host: String, port: Int, username: String, password: String): IbcServiceGrpc.IbcServiceImplBase() {
    private val ops = CordaRPCClient(NetworkHostAndPort(host, port))
            .start(username, password)
            .proxy

    override fun createGenesis(request: Participants, responseObserver: StreamObserver<GrpcSignedTransaction>) {
        val stx = ops.startFlow(::IbcGenesisCreateFlow, request.participantsList.map{it.into()}).returnValue.get()
        responseObserver.onNext(stx.into())
        responseObserver.onCompleted()
    }

    override fun createHostAndBank(request: StateRef, responseObserver: StreamObserver<GrpcSignedTransaction>) {
        val stx = ops.startFlow(::IbcHostAndBankCreateFlow, request.into()).returnValue.get()
        responseObserver.onNext(stx.into())
        responseObserver.onCompleted()
    }

    override fun allocateFund(request: AllocateFundRequest, responseObserver: StreamObserver<jp.datachain.corda.ibc.grpc.SignedTransaction>) {
        val stx = ops.startFlow(::IbcFundAllocateFlow,
                request.baseId.into(),
                request.owner.into(),
                Denom(request.denom),
                Amount(request.amount.toBigDecimal())
        ).returnValue.get()
        responseObserver.onNext(stx.into())
        responseObserver.onCompleted()
    }

    override fun createClient(request: CreateClientRequest, responseObserver: StreamObserver<jp.datachain.corda.ibc.grpc.SignedTransaction>) {
        val stx = ops.startFlow(::IbcClientCreateFlow,
                request.baseId.into(),
                request.id.into(),
                request.clientType.into(),
                request.consensusState.into()
        ).returnValue.get()
        responseObserver.onNext(stx.into())
        responseObserver.onCompleted()
    }

    override fun queryHost(request: StateRef, responseObserver: StreamObserver<GrpcHost>) {
        val hostAndRef = ops.vaultQueryBy<Host>(
                QueryCriteria.LinearStateQueryCriteria(
                        externalId = listOf(request.into().toString())
                )
        ).states.single()
        val reply: GrpcHost = hostAndRef.state.data.into()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun queryBank(request: StateRef, responseObserver: StreamObserver<GrpcBank>) {
        val bankAndRef = ops.vaultQueryBy<Bank>(
                QueryCriteria.LinearStateQueryCriteria(
                        externalId = listOf(request.into().toString())
                )
        ).states.single()
        val reply: GrpcBank = bankAndRef.state.data.into()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}