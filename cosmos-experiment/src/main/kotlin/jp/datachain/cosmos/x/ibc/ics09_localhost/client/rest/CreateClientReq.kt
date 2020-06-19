package jp.datachain.cosmos.x.ibc.ics09_localhost.client.rest

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.corda.ibc.cosmos.CosmosRequest
import jp.datachain.corda.ibc.cosmos.ReqPath
import jp.datachain.cosmos.types.rest.BaseReq

@ReqPath("ibc/clients/localhost")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
class CreateClientReq : CosmosRequest {
    override var baseReq: BaseReq? = null
}
