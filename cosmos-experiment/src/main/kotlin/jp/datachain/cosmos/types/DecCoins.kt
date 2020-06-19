package jp.datachain.cosmos.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class DecCoin(
        val denom: String?,
        val amount: Int
)

typealias DecCoins = List<DecCoin>