package jp.datachain.corda.ibc.grpc_adapter

import io.grpc.BindableService
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
        val listenPort = args[4]
        val baseId: StateRef? = if (args.size == 6) StateRef(SecureHash.parse(args[5]), 0) else null

        val serverBuilder = ServerBuilder.forPort(listenPort.toInt())

        val adminService = AdminService()
        serverBuilder.addService(adminService)

        val opsReadyServices = mutableListOf<CordaRPCOpsReady>()
        opsReadyServices += NodeService(hostname, port, username, password)
        opsReadyServices += GenesisService(hostname, port, username, password)
        baseId?.let{
            opsReadyServices += HostService(hostname, port, username, password, it)
            opsReadyServices += BankService(hostname, port, username, password, it)

            opsReadyServices += ClientTxService(hostname, port, username, password, it)
            opsReadyServices += ConnectionTxService(hostname, port, username, password, it)
            opsReadyServices += ChannelTxService(hostname, port, username, password, it)
            opsReadyServices += TransferTxService(hostname, port, username, password, it)

            opsReadyServices += ClientQueryService(hostname, port, username, password, it)
            opsReadyServices += ConnectionQueryService(hostname, port, username, password, it)
            opsReadyServices += ChannelQueryService(hostname, port, username, password, it)
        }
        opsReadyServices.forEach{serverBuilder.addService(it as BindableService)}

        val server = serverBuilder.build()
        adminService.server = server

        server.start()
        server.awaitTermination()

        opsReadyServices.forEach(CordaRPCOpsReady::close)

        println("Bye-bye.")
    }
}