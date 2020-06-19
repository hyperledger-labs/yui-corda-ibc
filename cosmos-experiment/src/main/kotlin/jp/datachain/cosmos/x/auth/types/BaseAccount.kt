package jp.datachain.cosmos.x.auth.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class BaseAccount(
        val address: String,
        val publicKey: String?,
        val accountNumber: Int?,
        val sequence: Int?
)