package jp.datachain.cosmos.x.ibc.ics07_tendermint.client.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.corda.ibc.cosmos.CosmosRequest
import jp.datachain.corda.ibc.cosmos.ReqPath
import jp.datachain.cosmos.types.rest.BaseReq

@ReqPath("ibc/clients/tendermint")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class CreateClientReq(
        val clientID: String,
        val chainID: String,
        val consensusState: JsonNode, /*github.com/cosmos/cosmos-sdk/x/ibc/07-tendermint/types.Header*/
        val trustingPeriod: String /*time.Duration*/,
        val unbondingPeriod: String /*time.Duration*/,
        val maxClockDrift: String /*time.Duration*/
) : CosmosRequest {
    override var baseReq: BaseReq? = null
}
