package jp.datachain.cosmos.x.ibc.ics04_channel.client.rest

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.corda.ibc.cosmos.CosmosRequest
import jp.datachain.corda.ibc.cosmos.ReqPath
import jp.datachain.cosmos.types.rest.BaseReq
import jp.datachain.cosmos.x.ibc.ics23_commitment.types.MerkleProof
import jp.datachain.cosmos.x.ibc.types.Order

@ReqPath("ibc/channels/open-try")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ChannelOpenTryReq(
        val portID: String,
        val channelID: String,
        val version: String,
        val channelOrder: Order,
        val connectionHops: List<String>,
        val counterpartyPortID: String,
        val counterpartyChannelID: String,
        val counterpartyVersion: String,
        val proofInit: MerkleProof,
        val proofHeight: String /*uint64*/
) : CosmosRequest {
    override var baseReq: BaseReq? = null
}