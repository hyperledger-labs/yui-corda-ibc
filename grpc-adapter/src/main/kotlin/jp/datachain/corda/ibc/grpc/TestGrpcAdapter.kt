package jp.datachain.corda.ibc.grpc

import io.grpc.ManagedChannelBuilder
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateRef
import net.corda.core.utilities.NetworkHostAndPort

object TestGrpcAdapter {
    @JvmStatic
    fun main(args: Array<String>) {
        for (port in listOf(10003, 10006, 10009)) {
            CordaRPCClient(NetworkHostAndPort("localhost", port))
                    .start("user1", "test")
                    .close()
        }

        val channel = ManagedChannelBuilder.forTarget("localhost:9999")
                .usePlaintext()
                .build()
        val cordaService = CordaServiceGrpc.newBlockingStub(channel)
        val ibcService = IbcServiceGrpc.newBlockingStub(channel)

        val partyMap = listOf("PartyA", "PartyB", "Notary").map{it to
                cordaService.partiesFromName(PartiesFromNameRequest.newBuilder()
                        .setName(it)
                        .setExactMatch(false)
                        .build()).partiesList.single()
        }.toMap()

        val stxGenesis = ibcService.createGenesis(Participants.newBuilder()
                .addAllParticipants(partyMap.values)
                .build()).into()

        val baseId = StateRef(txhash = stxGenesis.id, index = 0)

        val stxHostAndBank = ibcService.createHostAndBank(baseId.into()).into()

        val host = ibcService.queryHost(baseId.into()).into()
        val bank = ibcService.queryBank(baseId.into()).into()

        val stxFund = ibcService.allocateFund(AllocateFundRequest.newBuilder()
                .setBaseId(baseId.into())
                .setOwner(partyMap["PartyA"]!!.owningKey)
                .setDenom("USD")
                .setAmount("10.5")
                .build()).into()
        val bankAfterFund = ibcService.queryBank(baseId.into()).into()

        val stxClient = ibcService.createClient(CreateClientRequest.newBuilder()
                .setBaseId(baseId.into())
                .setId(Identifier("testclient").into())
                .setClientType(ClientType.CordaClient.into())
                .setConsensusState(host.getConsensusState(host.getCurrentHeight()).into().asSuper())
                .build()).into()

        println(host)
        println(bank)

        assert(stxHostAndBank.tx.outputsOfType<Host>().single() == host)
        assert(stxHostAndBank.tx.outputsOfType<Bank>().single() == bank)
        assert(stxFund.tx.outputsOfType<Bank>().single() == bankAfterFund)
    }
}