package jp.datachain.cosmos.x.auth.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.amino.DisfixWrapper

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class StdTx (
        val msg: List<DisfixWrapper>,
        val fee: StdFee,
        val signatures: List<StdSignature>?,
        val memo: String
) {
    inline fun <reified T> msgs() = msg.map{it.valueAs<T>()}
}