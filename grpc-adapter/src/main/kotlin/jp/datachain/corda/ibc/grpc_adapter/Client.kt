package jp.datachain.corda.ibc.grpc_adapter

import io.grpc.ManagedChannelBuilder
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.grpc.*
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateRef
import net.corda.core.utilities.NetworkHostAndPort
import io.grpc.StatusRuntimeException
import net.corda.core.crypto.SecureHash
import net.corda.core.utilities.toHex

object Client {
    @JvmStatic
    fun main(args: Array<String>) {
        warmUpSerialization()
        when (args[0]) {
            "shutdown" -> shutdown(args[1])
            "createGenesis" -> createGenesis(args[1], args[2])
            "createHost" -> createHost(args[1], args[2])
            "allocateFund" -> allocateFund(args[1], args[2], args[3])
            "executeTest" -> executeTest(args[1], args[2])
        }
    }

    private fun warmUpSerialization() {
        for (port in listOf(10003, 10006, 10009)) {
            CordaRPCClient(NetworkHostAndPort("localhost", port))
                    .start("user1", "test")
                    .close()
        }
    }

    private fun connectGrpc(endpoint: String) = ManagedChannelBuilder.forTarget(endpoint)
            .usePlaintext()
            .build()

    private fun shutdown(endpoint: String) {
        val channel = connectGrpc(endpoint)
        val adminService = AdminServiceGrpc.newBlockingStub(channel)

        try {
            adminService.shutdown(Void.getDefaultInstance())
        } catch (e: StatusRuntimeException) {
            println(e)
        }
    }

    private fun createGenesis(endpoint: String, partyName: String) {
        val channel = connectGrpc(endpoint)
        val nodeService = NodeServiceGrpc.newBlockingStub(channel)
        val ibcService = IbcServiceGrpc.newBlockingStub(channel)

        val partyMap = listOf(partyName, "Notary").map {
            it to
                    nodeService.partiesFromName(Operation.PartiesFromNameRequest.newBuilder()
                            .setName(it)
                            .setExactMatch(false)
                            .build()).partiesList.single()
        }.toMap()

        val stxGenesis = ibcService.createGenesis(Operation.CreateGenesisRequest.newBuilder()
                .addAllParticipants(partyMap.values)
                .build()).into()

        val baseId = StateRef(txhash = stxGenesis.id, index = 0)

        println(baseId.txhash.bytes.toHex())
    }

    private fun createHost(endpoint: String, baseHash: String) {
        val channel = connectGrpc(endpoint)
        val ibcService = IbcServiceGrpc.newBlockingStub(channel)

        val baseId = StateRef(txhash = SecureHash.parse(baseHash), index = 0)

        ibcService.createHostAndBank(baseId.into())
    }

    private fun allocateFund(endpoint: String, baseHash: String, partyName: String) {
        val channel = connectGrpc(endpoint)
        val nodeService = NodeServiceGrpc.newBlockingStub(channel)
        val ibcService = IbcServiceGrpc.newBlockingStub(channel)

        val baseId = StateRef(txhash = SecureHash.parse(baseHash), index = 0)

        val party = nodeService.partiesFromName(Operation.PartiesFromNameRequest.newBuilder()
                .setName(partyName)
                .setExactMatch(false)
                .build()).partiesList.single()

        ibcService.allocateFund(Operation.AllocateFundRequest.newBuilder()
                .setBaseId(baseId.into())
                .setOwner(party.owningKey)
                .setDenom("USD")
                .setAmount("100")
                .build()).into()

        println(baseId.txhash.bytes.toHex())
    }

    private fun executeTest(endpointA: String, endpointB: String) {}
}