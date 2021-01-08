package jp.datachain.corda.ibc.grpc_adapter

import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.flows.IbcFundAllocateFlow
import jp.datachain.corda.ibc.flows.IbcGenesisCreateFlow
import jp.datachain.corda.ibc.flows.IbcHostAndBankCreateFlow
import jp.datachain.corda.ibc.grpc.*
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Denom
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.messaging.startFlow

class CordaIbcService(host: String, port: Int, username: String, password: String): IbcServiceGrpc.IbcServiceImplBase() {
    private val ops = CordaRPCClient(NetworkHostAndPort(host, port))
            .start(username, password)
            .proxy

    override fun createGenesis(request: Operation.CreateGenesisRequest, responseObserver: StreamObserver<CordaTypes.SignedTransaction>) {
        val stx = ops.startFlow(::IbcGenesisCreateFlow, request.participantsList.map{it.into()}).returnValue.get()
        responseObserver.onNext(stx.into())
        responseObserver.onCompleted()
    }

    override fun createHostAndBank(request: CordaTypes.StateRef, responseObserver: StreamObserver<CordaTypes.SignedTransaction>) {
        val stx = ops.startFlow(::IbcHostAndBankCreateFlow, request.into()).returnValue.get()
        responseObserver.onNext(stx.into())
        responseObserver.onCompleted()
    }

    override fun allocateFund(request: Operation.AllocateFundRequest, responseObserver: StreamObserver<CordaTypes.SignedTransaction>) {
        val stx = ops.startFlow(::IbcFundAllocateFlow,
                request.baseId.into(),
                request.owner.into(),
                Denom(request.denom),
                Amount(request.amount)
        ).returnValue.get()
        responseObserver.onNext(stx.into())
        responseObserver.onCompleted()
    }
}