package jp.datachain.tendermint.crypto.merkle

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ProofOp(
        val type: String?,
        val key: ByteArray?,
        val data: ByteArray?
)