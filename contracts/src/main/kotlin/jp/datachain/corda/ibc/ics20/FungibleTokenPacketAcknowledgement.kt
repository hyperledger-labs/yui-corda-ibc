package jp.datachain.corda.ibc.ics20

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class FungibleTokenPacketAcknowledgement private constructor(val success: Boolean, val error: String? = null) {
    constructor(): this(success = true) {}
    constructor(error: String): this(success = false, error = error) {}

    companion object {
        fun decode(bytes: ByteArray) = jacksonObjectMapper().convertValue<FungibleTokenPacketAcknowledgement>(bytes)
    }

    fun encode() = jacksonObjectMapper().writeValueAsBytes(this)
}