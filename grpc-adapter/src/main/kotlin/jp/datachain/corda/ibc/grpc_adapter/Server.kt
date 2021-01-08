package jp.datachain.corda.ibc.grpc_adapter

import io.grpc.ServerBuilder
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash

object Server {
    @JvmStatic
    fun main(args: Array<String>) {
        val hostname = args[0]
        val port = args[1].toInt()
        val username = args[2]
        val password = args[3]
        val baseId: StateRef? = if (args.size == 5) StateRef(SecureHash.parse(args[4]), 0) else null

        val adminService = AdminService()
        val serverBuilder = ServerBuilder.forPort(9999)
                .addService(adminService)
                .addService(CordaNodeService(hostname, port, username, password))
                .addService(CordaIbcService(hostname, port, username, password))
        baseId?.let{
            serverBuilder.addService(ClientTxService(hostname, port, username, password, it))
            serverBuilder.addService(ConnectionTxService(hostname, port, username, password, it))
            serverBuilder.addService(ChannelTxService(hostname, port, username, password, it))
            serverBuilder.addService(TransferTxService(hostname, port, username, password, it))

            serverBuilder.addService(HostAndBankQueryService(hostname, port, username, password, it))
            serverBuilder.addService(ClientQueryService(hostname, port, username, password, it))
            serverBuilder.addService(ConnectionQueryService(hostname, port, username, password, it))
            serverBuilder.addService(ChannelQueryService(hostname, port, username, password, it))
        }
        val server = serverBuilder.build()
        adminService.server = server

        server.start()
        server.awaitTermination()

        println("Bye-bye.")
    }
}