package jp.datachain.corda.ibc.ics24

import net.corda.core.serialization.CordaSerializable
import java.util.UUID

@CordaSerializable
data class Identifier(val id: String): Comparable<Identifier> {
    init {
        require(id.matches("[0-9a-zA-Z._+\\-#\\[\\]<>]+".toRegex()))
    }

    fun toUUID() = UUID.nameUUIDFromBytes(id.toByteArray(charset = Charsets.US_ASCII))!!

    override fun compareTo(other: Identifier) = id.compareTo(other.id)
}