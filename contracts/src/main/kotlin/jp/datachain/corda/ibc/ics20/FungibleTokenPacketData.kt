package jp.datachain.corda.ibc.ics20

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.security.PublicKey

data class FungibleTokenPacketData(
        val denomination: Denom,
        val amount: Amount,
        val sender: PublicKey,
        val receiver: PublicKey
) {
    companion object {
        fun decode(bytes: ByteArray) = jacksonObjectMapper().convertValue<FungibleTokenPacketData>(bytes)
    }

    fun encode() = jacksonObjectMapper().writeValueAsBytes(this)
}