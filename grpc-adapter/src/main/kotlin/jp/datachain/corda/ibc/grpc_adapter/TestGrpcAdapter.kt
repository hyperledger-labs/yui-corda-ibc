package jp.datachain.corda.ibc.grpc_adapter

import ibc.core.client.v1.Client
import ibc.core.client.v1.MsgGrpc
import io.grpc.ManagedChannelBuilder
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.grpc.*
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics24.Host
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateRef
import net.corda.core.utilities.NetworkHostAndPort
import com.google.protobuf.Any

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
        val nodeService = NodeServiceGrpc.newBlockingStub(channel)
        val ibcService = IbcServiceGrpc.newBlockingStub(channel)
        val clientMsgService = MsgGrpc.newBlockingStub(channel)

        val partyMap = listOf("PartyA", "PartyB", "Notary").map{it to
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

        val stxFund = ibcService.allocateFund(Corda.AllocateFundRequest.newBuilder()
                .setBaseId(baseId.into())
                .setOwner(partyMap["PartyA"]!!.owningKey)
                .setDenom("USD")
                .setAmount("10.5")
                .build()).into()
        val bankAfterFund = ibcService.queryBank(baseId.into()).into()

        clientMsgService.createClient(Client.MsgCreateClient.newBuilder()
                .setClientId("testclient")
                .setConsensusState(Any.pack(host.getConsensusState(host.getCurrentHeight()).consensusState))
                .build())

        println(host)
        println(bank)

        assert(stxHostAndBank.tx.outputsOfType<Host>().single() == host)
        assert(stxHostAndBank.tx.outputsOfType<Bank>().single() == bank)
        assert(stxFund.tx.outputsOfType<Bank>().single() == bankAfterFund)
    }
}