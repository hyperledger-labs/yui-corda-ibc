package jp.datachain.cosmos.x.ibc.ics20_transfer.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.cosmos.types.Coins

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class FungibleTokenPacketData(
        // the tokens to be transferred
        val amount: Coins,
        // the sender address
        val sender: String?,
        // the recipient address on the destination chain
        val receiver: String?
)
