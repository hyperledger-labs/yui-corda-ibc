package jp.datachain.cosmos.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class Coin(
        val denom: String?,
        val amount: String /*Int*/
)

typealias Coins = List<Coin>
