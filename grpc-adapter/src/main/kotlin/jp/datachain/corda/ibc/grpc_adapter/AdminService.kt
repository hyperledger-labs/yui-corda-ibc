package jp.datachain.corda.ibc.grpc_adapter

import com.google.protobuf.Empty
import ibc.lightclients.corda.v1.AdminServiceGrpc
import io.grpc.Server
import io.grpc.stub.StreamObserver
import java.lang.NullPointerException

class AdminService: AdminServiceGrpc.AdminServiceImplBase() {
    var server: Server? = null

    override fun shutdown(request: Empty, responseObserver: StreamObserver<Empty>) {
        if (server != null) {
            server!!.shutdown()
            responseObserver.onNext(Empty.getDefaultInstance())
            responseObserver.onCompleted()
        } else {
            responseObserver.onError(NullPointerException())
        }
    }
}