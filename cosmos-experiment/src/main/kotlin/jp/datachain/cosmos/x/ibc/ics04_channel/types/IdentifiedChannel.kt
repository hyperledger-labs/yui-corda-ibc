package jp.datachain.cosmos.x.ibc.ics04_channel.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.cosmos.x.ibc.types.Order
import jp.datachain.cosmos.x.ibc.types.State

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class IdentifiedChannel(
        val id: String,
        val portID: String,
        val state: State,
        val ordering: Order,
        val counterparty: Counterparty,
        val connectionHops: List<String>,
        val version: String
)