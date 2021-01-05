package jp.datachain.corda.ibc.grpc_adapter

import io.grpc.ServerBuilder
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash

object GrpcAdapter {
    @JvmStatic
    fun main(args: Array<String>) {
        val hostname = args[0]
        val port = args[1].toInt()
        val username = args[2]
        val password = args[3]
        val baseId: StateRef? = if (args.size == 5) StateRef(SecureHash.parse(args[4]), 0) else null

        val adminService = GrpcAdminService()
        val serverBuilder = ServerBuilder.forPort(9999)
                .addService(adminService)
                .addService(GrpcNodeService(hostname, port, username, password))
                .addService(GrpcIbcService(hostname, port, username, password))
        baseId?.let{serverBuilder.addService(ClientMsgService(hostname, port, username, password, it))}
        val server = serverBuilder.build()
        adminService.server = server

        server.start()
        server.awaitTermination()

        println("Bye-bye.")
    }
}