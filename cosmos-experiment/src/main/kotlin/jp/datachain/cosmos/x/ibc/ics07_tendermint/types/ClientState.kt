package jp.datachain.cosmos.x.ibc.ics07_tendermint.types

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ClientState(
        // Client ID
        val id: String,

        // Duration of the period since the LastestTimestamp during which the
        // submitted headers are valid for upgrade
        val trustingPeriod: String, /*time.Duration*/

        // Duration of the staking unbonding period
        val unbondingPeriod: String, /*time.Duration*/

        // MaxClockDrift defines how much new (untrusted) header's Time can drift into
        // the future.
        @JsonProperty("MaxClockDrift")
        val maxClockDrift: String, /*time.Duration*/

        // Block height when the client was frozen due to a misbehaviour
        val frozenHeight: String, /*uint64*/

        // Last Header that was stored by client
        val lastHeader: JsonNode /*github.com/cosmos/cosmos-sdk/x/ibc/07-tendermint/types.Header*/
)
