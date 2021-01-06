package jp.datachain.corda.ibc.grpc_adapter

import io.grpc.ManagedChannelBuilder
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.grpc.*
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics24.Host
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateRef
import net.corda.core.utilities.NetworkHostAndPort
import io.grpc.StatusRuntimeException
import net.corda.core.utilities.toHex

object Client {
    @JvmStatic
    fun main(args: Array<String>) {
        warmUpSerialization()
        when (args[0]) {
            "shutdown" -> shutdown(args[1])
            "createHost" -> {
                createHost(args[1], args[2])
            }
            "executeTest" -> {
                executeTest(args[1], args[2])
            }
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

    private fun createHost(endpoint: String, partyName: String) {
        val channel = connectGrpc(endpoint)
        val nodeService = NodeServiceGrpc.newBlockingStub(channel)
        val ibcService = IbcServiceGrpc.newBlockingStub(channel)

        val partyMap = listOf(partyName, "Notary").map {
            it to
                    nodeService.partiesFromName(Corda.PartiesFromNameRequest.newBuilder()
                            .setName(it)
                            .setExactMatch(false)
                            .build()).partiesList.single()
        }.toMap()

        val stxGenesis = ibcService.createGenesis(Corda.Participants.newBuilder()
                .addAllParticipants(partyMap.values)
                .build()).into()

        val baseId = StateRef(txhash = stxGenesis.id, index = 0)

        val stxHostAndBank = ibcService.createHostAndBank(baseId.into()).into()

        val host = ibcService.queryHost(baseId.into()).into()
        val bank = ibcService.queryBank(baseId.into()).into()
        assert(stxHostAndBank.tx.outputsOfType<Host>().single() == host)
        assert(stxHostAndBank.tx.outputsOfType<Bank>().single() == bank)

        val stxFund = ibcService.allocateFund(Corda.AllocateFundRequest.newBuilder()
                .setBaseId(baseId.into())
                .setOwner(partyMap[partyName]!!.owningKey)
                .setDenom("USD")
                .setAmount("100")
                .build()).into()

        val bankAfterFund = ibcService.queryBank(baseId.into()).into()
        assert(stxFund.tx.outputsOfType<Bank>().single() == bankAfterFund)

        println(baseId.txhash.bytes.toHex())
    }

    private fun executeTest(endpointA: String, endpointB: String) {}
}