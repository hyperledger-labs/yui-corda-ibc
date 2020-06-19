package jp.datachain.cosmos.x.ibc.ics09_localhost.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.cosmos.types.AccAddress

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class MsgCreateClient(
        val address: AccAddress
)