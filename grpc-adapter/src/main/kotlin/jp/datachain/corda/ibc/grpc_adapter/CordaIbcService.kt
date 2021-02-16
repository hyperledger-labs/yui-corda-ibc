package jp.datachain.corda.ibc.grpc_adapter

import ibc.lightclients.corda.v1.CordaTypes
import ibc.lightclients.corda.v1.IbcServiceGrpc
import ibc.lightclients.corda.v1.Operation
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.flows.IbcFundAllocateFlow
import jp.datachain.corda.ibc.flows.IbcGenesisCreateFlow
import jp.datachain.corda.ibc.flows.IbcHostAndBankCreateFlow
import jp.datachain.corda.ibc.ics20.Address
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Denom
import net.corda.core.messaging.startFlow

class CordaIbcService(host: String, port: Int, username: String, password: String): IbcServiceGrpc.IbcServiceImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
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
                Address(request.owner),
                Denom(request.denom),
                Amount(request.amount)
        ).returnValue.get()
        responseObserver.onNext(stx.into())
        responseObserver.onCompleted()
    }
}