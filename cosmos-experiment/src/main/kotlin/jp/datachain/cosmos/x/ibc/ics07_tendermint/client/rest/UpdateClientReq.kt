package jp.datachain.cosmos.x.ibc.ics07_tendermint.client.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.corda.ibc.cosmos.CosmosRequest
import jp.datachain.corda.ibc.cosmos.ReqPath
import jp.datachain.cosmos.types.rest.BaseReq

@ReqPath("ibc/clients/%s/update")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class UpdateClientReq(
        val header: JsonNode /*github.com/cosmos/cosmos-sdk/x/ibc/07-tendermint/types.Header*/
) : CosmosRequest {
    override var baseReq: BaseReq? = null
}
