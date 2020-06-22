package jp.datachain.corda.ibc.cosmos

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import jp.datachain.amino.DisfixWrapper
import jp.datachain.cosmos.types.Coin
import jp.datachain.cosmos.x.auth.types.BaseAccount
import jp.datachain.cosmos.x.auth.types.StdTx
import jp.datachain.cosmos.x.ibc.ics02_client.types.ConsensusStateResponse
import jp.datachain.cosmos.x.ibc.ics03_connection.client.rest.ConnectionOpenAckReq
import jp.datachain.cosmos.x.ibc.ics03_connection.client.rest.ConnectionOpenConfirmReq
import jp.datachain.cosmos.x.ibc.ics03_connection.client.rest.ConnectionOpenInitReq
import jp.datachain.cosmos.x.ibc.ics03_connection.client.rest.ConnectionOpenTryReq
import jp.datachain.cosmos.x.ibc.ics03_connection.types.ConnectionResponse
import jp.datachain.cosmos.x.ibc.ics04_channel.client.rest.*
import jp.datachain.cosmos.x.ibc.ics04_channel.types.ChannelResponse
import jp.datachain.cosmos.x.ibc.ics04_channel.types.PacketResponse
import jp.datachain.cosmos.x.ibc.ics07_tendermint.client.rest.UpdateClientReq
import jp.datachain.cosmos.x.ibc.ics20_transfer.client.rest.TransferTxReq
import jp.datachain.cosmos.x.ibc.ics20_transfer.types.FungibleTokenPacketData
import jp.datachain.cosmos.x.ibc.ics23_commitment.types.MerklePrefix
import jp.datachain.cosmos.x.ibc.types.Order
import jp.datachain.cosmos.x.ibc.types.State
import jp.datachain.cosmos.x.ibc.ics09_localhost.client.rest.CreateClientReq as CreateClientReqIcs09
import jp.datachain.cosmos.x.ibc.ics07_tendermint.client.rest.CreateClientReq as CreateClientReqIcs07

object Experiment {
    val HOST = "localhost"
    val PORT = 1317
    val CHAIN_ID = "hogechain"
    val KEYRING_BACKEND = "test"

    val ALICE = "cosmos1m39ny60g5m2fk5tmuc34yq7tdg6nl7yj2s6gt9"
    val BOB = "cosmos13mrncy7vakpj0aksz5z4myfwgc40m8fds8axtp"

    val TRUSTING_PERIOD = 336L * 3600 * 1000 * 1000 * 1000
    val UNBONDING_PERIOD = 504L * 3600 * 1000 * 1000 * 1000
    val MAX_CLOCK_DRIFT = 10L * 1000 * 1000 * 1000

    val CLIENT_A = "clientaaa"
    val CLIENT_B = "clientbbb"

    val CONNECTION_A = "connectionaaa"
    val CONNECTION_B = "connectionbbb"

    val CONN_VERSIONS = listOf("1.0.0")

    val PORT_ID = "transfer"
    val CHANNEL_A = "channelaaaat"
    val CHANNEL_B = "channelbbbbt"

    val CHAN_VERSION = "ics20-1"
    val ORDER = Order.ORDERED

    val DENOM = "hogecoin"

    val sortedMapper = ObjectMapper().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)

    @JvmStatic
    fun main(args: Array<String>) {
        val client = CosmosRESTClient(HOST, PORT)

        val alice = client.query("auth/accounts/$ALICE").resultAs<DisfixWrapper>().valueAs<BaseAccount>()
        println("alice: ${alice}")

        val bob = client.query("auth/accounts/$BOB").resultAs<DisfixWrapper>().valueAs<BaseAccount>()
        println("bob: ${bob}")

        val transactor = CosmosTransactor(alice, CHAIN_ID)
        val bobTransactor = CosmosTransactor(bob, CHAIN_ID)
        println("prepare transactor OK")

        val signer = CosmosSigner(alice.address, CHAIN_ID, KEYRING_BACKEND)
        val bobSigner = CosmosSigner(bob.address, CHAIN_ID, KEYRING_BACKEND)
        println("prepare signer OK")

        val triple = Triple(client, transactor, signer)
        val bobTriple = Triple(client, bobTransactor, bobSigner)

        var headerB = client.query("ibc/header").resultAs<DisfixWrapper>().value
        triple.sendTx(CreateClientReqIcs07(
                clientID = CLIENT_A,
                chainID = CHAIN_ID,
                consensusState = headerB,
                trustingPeriod =  TRUSTING_PERIOD.toString(),
                unbondingPeriod = UNBONDING_PERIOD.toString(),
                maxClockDrift = MAX_CLOCK_DRIFT.toString()
        ))
        triple.waitForBlock()

        var headerA = client.query("ibc/header").resultAs<DisfixWrapper>().value
        triple.sendTx(CreateClientReqIcs07(
                clientID = CLIENT_B,
                chainID = CHAIN_ID,
                consensusState = headerA,
                trustingPeriod =  TRUSTING_PERIOD.toString(),
                unbondingPeriod = UNBONDING_PERIOD.toString(),
                maxClockDrift = MAX_CLOCK_DRIFT.toString()
        ))
        triple.waitForBlock()

        headerB = client.query("ibc/header").resultAs<DisfixWrapper>().value
        triple.sendTx(UpdateClientReq(headerB), CLIENT_A)
        triple.waitForBlock()

        triple.sendTx(ConnectionOpenInitReq(
                connectionID = CONNECTION_A,
                clientID = CLIENT_A,
                counterpartyClientID = CLIENT_B,
                counterpartyConnectionID = CONNECTION_B,
                counterpartyPrefix = MerklePrefix("ibc".toByteArray(charset = Charsets.US_ASCII))
        ))
        triple.waitForBlock()

        headerA = client.query("ibc/header").resultAs<DisfixWrapper>().value
        var connA = client.query("ibc/connections/${CONNECTION_A}?height=${headerA.height()-1}").resultAs<ConnectionResponse>()
        val consensusA = client.query("ibc/clients/${CLIENT_A}/consensus-state/${headerB.height()}?height=${headerA.height()-1}").resultAs<ConsensusStateResponse>()

        triple.sendTx(UpdateClientReq(headerA), CLIENT_B)
        triple.sendTx(ConnectionOpenTryReq(
                connectionID = CONNECTION_B,
                clientID = CLIENT_B,
                counterpartyClientID = CLIENT_A,
                counterpartyConnectionID = CONNECTION_A,
                counterpartyPrefix = MerklePrefix("ibc".toByteArray(charset = Charsets.US_ASCII)),
                counterpartyVersions = CONN_VERSIONS,
                proofInit = connA.proof!!,
                proofConsensus = consensusA.proof!!,
                proofHeight = headerA.height().toString(),
                consensusHeight = headerB.height().toString()
        ))
        triple.waitForBlock()

        headerB = client.query("ibc/header").resultAs<DisfixWrapper>().value
        var connB = client.query("ibc/connections/${CONNECTION_B}?height=${headerB.height()-1}").resultAs<ConnectionResponse>()
        val consensusB = client.query("ibc/clients/${CLIENT_B}/consensus-state/${headerA.height()}?height=${headerB.height()-1}").resultAs<ConsensusStateResponse>()

        triple.sendTx(UpdateClientReq(headerB), CLIENT_A)
        triple.sendTx(ConnectionOpenAckReq(
                proofTry = connB.proof!!,
                proofConsensus = consensusB.proof!!,
                proofHeight = headerB.height().toString(),
                consensusHeight = headerA.height().toString(),
                version = CONN_VERSIONS.single()
        ), CONNECTION_A)
        triple.waitForBlock()

        headerA = client.query("ibc/header").resultAs<DisfixWrapper>().value
        connA = client.query("ibc/connections/${CONNECTION_A}?height=${headerA.height()-1}").resultAs<ConnectionResponse>()

        triple.sendTx(UpdateClientReq(headerA), CLIENT_B)
        triple.sendTx(ConnectionOpenConfirmReq(
                proofAck = connA.proof!!,
                proofHeight = headerA.height().toString()
        ), CONNECTION_B)
        triple.waitForBlock()

        headerB = client.query("ibc/header").resultAs<DisfixWrapper>().value
        connB = client.query("ibc/connections/${CONNECTION_B}?height=${headerB.height()-1}").resultAs<ConnectionResponse>()
        assert(connB.connection.state == State.OPEN)

        triple.sendTx(UpdateClientReq(headerB), CLIENT_A)
        triple.sendTx(ChannelOpenInitReq(
                portID = PORT_ID,
                channelID = CHANNEL_A,
                version = CHAN_VERSION,
                channelOrder = ORDER,
                connectionHops = listOf(CONNECTION_A),
                counterpartyPortID = PORT_ID,
                counterpartyChannelID = CHANNEL_B
        ))
        triple.waitForBlock()

        headerA = client.query("ibc/header").resultAs<DisfixWrapper>().value
        var chanA = client.query("ibc/ports/${PORT_ID}/channels/${CHANNEL_A}?height=${headerA.height()-1}").resultAs<ChannelResponse>()

        triple.sendTx(UpdateClientReq(headerA), CLIENT_B)
        triple.sendTx(ChannelOpenTryReq(
                portID = PORT_ID,
                channelID = CHANNEL_B,
                version = CHAN_VERSION,
                channelOrder = ORDER,
                connectionHops = listOf(CONNECTION_B),
                counterpartyPortID = PORT_ID,
                counterpartyChannelID = CHANNEL_A,
                counterpartyVersion = CHAN_VERSION,
                proofInit = chanA.proof!!,
                proofHeight = headerA.height().toString()
        ))
        triple.waitForBlock()

        headerB = client.query("ibc/header").resultAs<DisfixWrapper>().value
        val chanB = client.query("ibc/ports/${PORT_ID}/channels/${CHANNEL_B}?height=${headerB.height()-1}").resultAs<ChannelResponse>()

        triple.sendTx(UpdateClientReq(headerB), CLIENT_A)
        triple.sendTx(ChannelOpenAckReq(
                counterpartyVersion = CHAN_VERSION,
                proofTry = chanB.proof!!,
                proofHeight = headerB.height().toString()
        ), PORT_ID, CHANNEL_A)
        triple.waitForBlock()

        headerA = client.query("ibc/header").resultAs<DisfixWrapper>().value
        chanA = client.query("ibc/ports/${PORT_ID}/channels/${CHANNEL_A}?height=${headerA.height()-1}").resultAs<ChannelResponse>()

        triple.sendTx(UpdateClientReq(headerA), CLIENT_B)
        triple.sendTx(ChannelOpenConfirmReq(
                proofAck = chanA.proof!!,
                proofHeight = headerA.height().toString()
        ), PORT_ID, CHANNEL_B)
        triple.waitForBlock()

        headerB = client.query("ibc/header").resultAs<DisfixWrapper>().value

        triple.sendTx(UpdateClientReq(headerB), CLIENT_A)
        triple.sendTx(TransferTxReq(
                destHeight = headerB.height().toString(),
                amount = listOf(Coin(denom = "$PORT_ID/$CHANNEL_B/$DENOM", amount = "10")),
                receiver = bob.address
        ), PORT_ID, CHANNEL_A)
        triple.waitForBlock()

        headerA = client.query("ibc/header").resultAs<DisfixWrapper>().value
        val packetResA = client.query("ibc/ports/$PORT_ID/channels/$CHANNEL_A/packets/6?height=${headerA.height()-1}").resultAs<PacketResponse>()
        println(packetResA)

        val packetDataA = DisfixWrapper(
                type = "ibc/transfer/PacketDataTransfer",
                value = sortedMapper.valueToTree(FungibleTokenPacketData(
                        amount = listOf(Coin(denom = "$PORT_ID/$CHANNEL_B/$DENOM", amount = "10")),
                        sender = alice.address,
                        receiver = bob.address
                ))
        )
        val jsonPacketA = sortedMapper.writeValueAsString(packetDataA)
        println("jsonPacketA: $jsonPacketA")
        val packetA = packetResA.packet.copy(
                data = jsonPacketA.toByteArray(),
                timeoutHeight = (headerB.height() + 1000).toString(),
                timeoutTimestamp = "0"
        )
        println("packetA: $packetA")

        triple.sendTx(UpdateClientReq(headerA), CLIENT_B)
        triple.sendTx(RecvPacketReq(
                packet = packetA,
                proofs = packetResA.proof!!,
                height = headerA.height().toString()
        ))
        bobTriple.sendTx(TransferTxReq(
                destHeight = headerA.height().toString(),
                amount = listOf(Coin(denom = "$PORT_ID/$CHANNEL_B/$DENOM", amount = "5")),
                receiver = alice.address
        ), PORT_ID, CHANNEL_B)
        triple.waitForBlock()

        headerB = client.query("ibc/header").resultAs<DisfixWrapper>().value
        val packetResB = client.query("ibc/ports/$PORT_ID/channels/$CHANNEL_B/packets/1?height=${headerB.height()-1}").resultAs<PacketResponse>()
        println(packetResB)

        val packetDataB = DisfixWrapper(
                type = "ibc/transfer/PacketDataTransfer",
                value = sortedMapper.valueToTree(FungibleTokenPacketData(
                        amount = listOf(Coin(denom = "$PORT_ID/$CHANNEL_B/$DENOM", amount = "5")),
                        sender = bob.address,
                        receiver = alice.address
                ))
        )
        val jsonPacketB = sortedMapper.writeValueAsString(packetDataB)
        println("jsonPacketB: $jsonPacketB")
        val packetB = packetResB.packet.copy(
                data = jsonPacketB.toByteArray(),
                timeoutHeight = (headerA.height() + 1000).toString(),
                timeoutTimestamp = "0"
        )
        println("packetB: $packetB")

        triple.sendTx(UpdateClientReq(headerB), CLIENT_A)
        triple.sendTx(RecvPacketReq(
                packet = packetB,
                proofs = packetResB.proof!!,
                height = headerB.height().toString()
        ))
        triple.waitForBlock()
    }

    inline fun <reified REQ: CosmosRequest> Triple<CosmosRESTClient, CosmosTransactor, CosmosSigner>.sendTx(req: REQ, vararg pathArgs: String) {
        val reqName = REQ::class.simpleName!!

        val unsignedTx = first.request(second(req), *pathArgs)
        println("request $reqName OK")
        println(unsignedTx)

        val signedTx = third(unsignedTx)
        println("sign $reqName OK")
        println(signedTx)

        val txResponse = first.broadcast(signedTx.valueAs<StdTx>(), "block")
        println("broadcast $reqName OK")
        println(txResponse)
    }

    fun Triple<CosmosRESTClient, CosmosTransactor, CosmosSigner>.waitForBlock() {
        val unsignedTx = first.request(second(CreateClientReqIcs09()))
        val signedTx = third(unsignedTx)
        first.broadcast(signedTx.valueAs<StdTx>(), "block")
    }

    fun JsonNode.height() = this["signed_header"]["header"]["height"].asText().toInt()
}