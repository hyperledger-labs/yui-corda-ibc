package jp.datachain.cosmos.x.ibc.ics04_channel.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class Counterparty(
        // port on the counterparty chain which owns the other end of the channel.
        val portID: String?,
        // channel end on the counterparty chain
        val channelID: String?
)