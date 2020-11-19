package jp.datachain.corda.ibc.grpc

import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.StateRef
import java.util.concurrent.TimeUnit

object GrpcAdapter {
    @JvmStatic
    fun main(args: Array<String>) {
        val hostname = args[0]
        val port = args[1].toInt()
        val username = args[2]
        val password = args[3]

        val server = ServerBuilder.forPort(9999)
                .addService(GrpcCordaService(hostname, port, username, password))
                .addService(GrpcIbcService(hostname, port, username, password))
                .build()
                .start()
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

        server.shutdown()
        server.awaitTermination(10, TimeUnit.SECONDS)

        println(host)
        println(bank)

        assert(stxHostAndBank.tx.outputsOfType<Host>().single() == host)
        assert(stxHostAndBank.tx.outputsOfType<Bank>().single() == bank)
        assert(stxFund.tx.outputsOfType<Bank>().single() == bankAfterFund)
    }
}