package jp.datachain.corda.ibc.lightclientd

import io.grpc.ServerBuilder

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        assert(args.size == 1)
        val port = args[0].toInt()

        ServerBuilder
            .forPort(port)
            .addService(LightClient())
            .build()
            .start()
            .awaitTermination()
    }
}