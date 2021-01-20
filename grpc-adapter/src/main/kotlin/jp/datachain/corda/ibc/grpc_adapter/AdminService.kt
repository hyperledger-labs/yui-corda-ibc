package jp.datachain.corda.ibc.grpc_adapter

import io.grpc.Server
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.grpc.AdminServiceGrpc
import jp.datachain.corda.ibc.grpc.Void
import java.lang.NullPointerException

class AdminService: AdminServiceGrpc.AdminServiceImplBase() {
    var server: Server? = null

    override fun shutdown(request: Void, responseObserver: StreamObserver<Void>) {
        if (server != null) {
            server!!.shutdown()
            responseObserver.onNext(Void.getDefaultInstance())
            responseObserver.onCompleted()
        } else {
            responseObserver.onError(NullPointerException())
        }
    }
}