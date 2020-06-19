package jp.datachain.cosmos.x.ibc.ics07_tendermint.types

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.cosmos.types.AccAddress

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class MsgCreateClient(
        val clientID: String,
        val header: JsonNode, /*github.com/cosmos/cosmos-sdk/x/ibc/07-tendermint/types.Header*/
        val trustingPeriod: String, /*time.Duration*/
        val unbondingPeriod: String, /*time.Duration*/
        val maxClockDrift: String, /*time.Duration*/
        val address: AccAddress
)
