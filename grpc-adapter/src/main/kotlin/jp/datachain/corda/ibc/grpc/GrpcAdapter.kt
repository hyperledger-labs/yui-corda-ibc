package jp.datachain.corda.ibc.grpc

import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import java.util.concurrent.TimeUnit

class GreeterImpl: GreeterGrpc.GreeterImplBase() {
    override fun sayHello(request: HelloRequest, responseObserver: StreamObserver<HelloReply>) {
        val reply = HelloReply.newBuilder()
                .setMessage("Hello, ${request.name!!}")
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}

object GrpcAdapter {
    @JvmStatic
    fun main(args: Array<String>) {
        val server = ServerBuilder.forPort(9999)
                .addService(GreeterImpl())
                .build()
                .start()
        val channel = ManagedChannelBuilder.forTarget("localhost:9999")
                .usePlaintext()
                .build()
        val blockingStub = GreeterGrpc.newBlockingStub(channel)
        val request = HelloRequest.newBuilder()
                .setName("Alice")
                .build()
        val response = blockingStub.sayHello(request)
        server.shutdown()
        server.awaitTermination(10, TimeUnit.SECONDS)
        println(response.message)
    }
}