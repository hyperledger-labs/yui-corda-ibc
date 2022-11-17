package jp.datachain.corda.ibc.grpc_adapter

import io.grpc.BindableService
import io.grpc.ServerBuilder

object Server {
    @JvmStatic
    fun main(args: Array<String>) {
        val hostname = args[0]
        val port = args[1].toInt()
        val username = args[2]
        val password = args[3]
        val listenPort = args[4]

        val serverBuilder = ServerBuilder.forPort(listenPort.toInt())

        val adminService = AdminService()
        serverBuilder.addService(adminService)

        val opsReadyServices: List<CordaRPCOpsReady> = listOf(
                NodeService(hostname, port, username, password),

                GenesisService(hostname, port, username, password),
                HostService(hostname, port, username, password),

                BankService(hostname, port, username, password),
                CashBankService(hostname, port, username, password),

                ClientTxService(hostname, port, username, password),
                ConnectionTxService(hostname, port, username, password),
                ChannelTxService(hostname, port, username, password),
                TransferTxService(hostname, port, username, password),

                ClientQueryService(hostname, port, username, password),
                ConnectionQueryService(hostname, port, username, password),
                ChannelQueryService(hostname, port, username, password)
        )
        opsReadyServices.forEach{serverBuilder.addService(it as BindableService)}

        val server = serverBuilder.build()
        adminService.server = server

        server.start()
        server.awaitTermination()

        opsReadyServices.forEach(CordaRPCOpsReady::close)

        println("Bye-bye.")
    }
}