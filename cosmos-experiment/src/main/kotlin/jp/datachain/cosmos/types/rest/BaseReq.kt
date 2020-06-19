package jp.datachain.cosmos.types.rest

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.cosmos.types.Coins
import jp.datachain.cosmos.types.DecCoins

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class BaseReq(
        val from: String,
        val memo: String? = null,
        val chainID: String,
        val accountNumber: String,
        val sequence: String,
        val fees: Coins? = null,
        val gasPrices: DecCoins? = null,
        val gas: String? = null,
        val gasAdjustment: String? = null,
        val simulate: Boolean? = null
)
