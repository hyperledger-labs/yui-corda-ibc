package jp.datachain.cosmos.x.ibc.ics09_localhost.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.corda.ibc.cosmos.ReqPath

@ReqPath("ibc/clients")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ClientState(
        val id: String,
        val chainID: String,
        val height: Int
)
