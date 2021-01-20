package jp.datachain.corda.ibc.grpc_adapter

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort

interface CordaRPCOpsReady {
    val ops: CordaRPCOps
    fun close()

    companion object {
        fun create(host: String, port: Int, username: String, password: String): CordaRPCOpsReady = object: CordaRPCOpsReady {
            private val connection = CordaRPCClient(NetworkHostAndPort(host, port)).start(username, password)
            override val ops get() = connection.proxy
            override fun close() = connection.close()
        }
    }
}