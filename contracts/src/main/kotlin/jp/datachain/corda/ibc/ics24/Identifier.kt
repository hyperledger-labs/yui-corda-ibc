package jp.datachain.corda.ibc.ics24

import net.corda.core.serialization.CordaSerializable
import java.util.UUID

@CordaSerializable
data class Identifier(val id: String) {
    init {
        // TODO: Check the ICS-24 spec and revise this condition
        assert(!id.matches(".*[^0-9a-zA-Z].*".toRegex()))
    }

    fun toUUID() = UUID.nameUUIDFromBytes(id.toByteArray(charset = Charsets.US_ASCII))!!
}