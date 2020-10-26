package jp.datachain.corda.ibc.ics20

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.corda.core.crypto.Crypto
import java.security.PublicKey

data class FungibleTokenPacketData(
        val denomination: Denom,
        val amount: Amount,
        val sender: PublicKey,
        val receiver: PublicKey
) {
    data class RawData(
            val denomination: Denom,
            val amount: Amount,
            val sender: ByteArray,
            val receiver: ByteArray)

    companion object {
        fun decode(bytes: ByteArray): FungibleTokenPacketData {
            val rawData: RawData = jacksonObjectMapper().readValue(bytes)
            return FungibleTokenPacketData(
                    rawData.denomination,
                    rawData.amount,
                    Crypto.decodePublicKey(rawData.sender),
                    Crypto.decodePublicKey(rawData.receiver))
        }
    }

    fun encode() = jacksonObjectMapper().writeValueAsBytes(RawData(
            denomination,
            amount,
            sender.encoded,
            receiver.encoded))
}