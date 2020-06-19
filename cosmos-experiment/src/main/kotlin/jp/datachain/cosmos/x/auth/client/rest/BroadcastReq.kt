package jp.datachain.cosmos.x.auth.client.rest

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.cosmos.x.auth.types.StdTx

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class BroadcastReq(val tx: StdTx, val mode: String)
