package jp.datachain.corda.ibc.cosmos

import com.fasterxml.jackson.databind.JsonNode
import jp.datachain.amino.DisfixWrapper
import jp.datachain.cosmos.x.auth.types.BaseAccount
import jp.datachain.cosmos.x.auth.types.StdTx
import jp.datachain.cosmos.x.ibc.ics02_client.types.ConsensusStateResponse
import jp.datachain.cosmos.x.ibc.ics03_connection.client.rest.ConnectionOpenInitReq
import jp.datachain.cosmos.x.ibc.ics03_connection.client.rest.ConnectionOpenTryReq
import jp.datachain.cosmos.x.ibc.ics03_connection.types.ConnectionResponse
import jp.datachain.cosmos.x.ibc.ics07_tendermint.client.rest.UpdateClientReq
import jp.datachain.cosmos.x.ibc.ics23_commitment.types.MerklePrefix
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

    val CLIENT_A = "clientalice"
    val CLIENT_B = "clientbob"

    val CONNECTION_A = "connectionalice"
    val CONNECTION_B = "connectionbob"

    val VERSIONS = listOf("1.0.0")

    @JvmStatic
    fun main(args: Array<String>) {
        val client = CosmosRESTClient(HOST, PORT)

        val alice = client.query("auth/accounts/$ALICE").resultAs<DisfixWrapper>().valueAs<BaseAccount>()
        println("alice: ${alice}")

        val bob = client.query("auth/accounts/$BOB").resultAs<DisfixWrapper>().valueAs<BaseAccount>()
        println("bob: ${bob}")

        val transactor = CosmosTransactor(alice, CHAIN_ID)
        println("prepare transactor OK")

        val signer = CosmosSigner(alice.address, CHAIN_ID, KEYRING_BACKEND)
        println("prepare signer OK")

        val triple = Triple(client, transactor, signer)

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

        /*
        val clients = client.query("ibc/clients").resultAs<Collection<DisfixWrapper>>()
        clients.forEach {
            if (it.type.contains("localhost")) {
                println("ICS09 client found!!!")
                val client = it.valueAs<ClientStateIcs09>()
                println(client)
            } else if (it.type.contains("tendermint")) {
                println("ICS07 client found!!!")
                val client = it.valueAs<ClientStateIcs07>()
                println(client)
            }
        }
        */

        triple.sendTx(ConnectionOpenInitReq(
                connectionID = CONNECTION_A,
                clientID = CLIENT_A,
                counterpartyClientID = CLIENT_B,
                counterpartyConnectionID = CONNECTION_B,
                counterpartyPrefix = MerklePrefix("ibc".toByteArray(charset = Charsets.US_ASCII))
        ))
        triple.waitForBlock()

        headerA = client.query("ibc/header").resultAs<DisfixWrapper>().value

        val connA = client.query("ibc/connections/${CONNECTION_A}?height=${headerA.height()-1}").resultAs<ConnectionResponse>()
        println("connA OK")
        println(connA)

        val consensus = client.query("ibc/clients/${CLIENT_A}/consensus-state/${headerB.height()}?height=${headerA.height()-1}").resultAs<ConsensusStateResponse>()
        println("consensus OK")
        println(consensus)

        triple.sendTx(UpdateClientReq(headerA), CLIENT_B)

        triple.sendTx(ConnectionOpenTryReq(
                connectionID = CONNECTION_B,
                clientID = CLIENT_B,
                counterpartyClientID = CLIENT_A,
                counterpartyConnectionID = CONNECTION_A,
                counterpartyPrefix = MerklePrefix("ibc".toByteArray(charset = Charsets.US_ASCII)),
                counterpartyVersions = VERSIONS,
                proofInit = connA.proof!!,
                proofConsensus = consensus.proof!!,
                proofHeight = headerA.height().toString(),
                consensusHeight = headerB.height().toString()
        ))

        val connB = client.query("ibc/connections/$CONNECTION_B").resultAs<ConnectionResponse>()
        println("connB OK")
        println(connB)
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