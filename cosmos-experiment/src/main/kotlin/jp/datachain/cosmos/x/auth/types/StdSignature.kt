package jp.datachain.cosmos.x.auth.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class StdSignature (
        val pubKey: ByteArray,
        val signature: ByteArray
)