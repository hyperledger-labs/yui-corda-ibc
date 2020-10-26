package jp.datachain.corda.ibc.ics20

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

data class FungibleTokenPacketAcknowledgement private constructor(val success: Boolean, val error: String? = null) {
    constructor(): this(success = true) {}
    constructor(error: String): this(success = false, error = error) {}

    companion object {
        fun decode(bytes: ByteArray): FungibleTokenPacketAcknowledgement = jacksonObjectMapper().readValue(bytes)
    }

    fun encode() = jacksonObjectMapper().writeValueAsBytes(this)
}