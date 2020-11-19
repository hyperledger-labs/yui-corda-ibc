package jp.datachain.corda.ibc.grpc

import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException

object ShutdownGrpcAdapter {
    @JvmStatic
    fun main(args: Array<String>) {
        val channel = ManagedChannelBuilder.forTarget("localhost:9999")
                .usePlaintext()
                .build()
        val adminService = AdminServiceGrpc.newBlockingStub(channel)
        try {
            adminService.shutdown(Void.getDefaultInstance())
        } catch (e: StatusRuntimeException) {
            println(e)
        }
    }
}