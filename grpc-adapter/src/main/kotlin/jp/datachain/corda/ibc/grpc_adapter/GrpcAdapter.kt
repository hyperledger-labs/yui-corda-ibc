package jp.datachain.corda.ibc.grpc_adapter

import io.grpc.ServerBuilder

object GrpcAdapter {
    @JvmStatic
    fun main(args: Array<String>) {
        val hostname = args[0]
        val port = args[1].toInt()
        val username = args[2]
        val password = args[3]

        val adminService = GrpcAdminService()
        val server = ServerBuilder.forPort(9999)
                .addService(adminService)
                .addService(GrpcCordaService(hostname, port, username, password))
                .addService(GrpcIbcService(hostname, port, username, password))
                .build()
        adminService.server = server

        server.start()
        server.awaitTermination()

        println("Bye-bye.")
    }
}