package jp.datachain.corda.ibc.cosmos

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.cosmos.types.rest.BaseReq

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
interface CosmosRequest {
    var baseReq: BaseReq?
}