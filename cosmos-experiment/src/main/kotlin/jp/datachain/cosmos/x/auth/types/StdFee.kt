package jp.datachain.cosmos.x.auth.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.cosmos.types.Coins

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class StdFee(
        val amount: Coins,
        val gas: String
)
