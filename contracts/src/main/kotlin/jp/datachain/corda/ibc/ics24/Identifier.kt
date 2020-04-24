package jp.datachain.corda.ibc.ics24

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.hexToByteArray
import net.corda.core.utilities.toHex
import java.lang.IllegalArgumentException
import java.util.*

@CordaSerializable
data class Identifier(val id: String) {
    init {
        assert(!id.matches(".*[^0-9a-zA-Z].*".toRegex()))
    }

    constructor(uniqueIdentifier: UniqueIdentifier) : this(uniqueIdentifier.toString().toByteArray(Charsets.UTF_8).toHex())

    fun toUniqueIdentifier() : UniqueIdentifier {
        val uniqueId = id.hexToByteArray().toString(Charsets.UTF_8)
        val externalId = uniqueId.substringBefore('_')
        val uuid = uniqueId.substringAfter('_')
        return UniqueIdentifier(externalId = externalId, id = UUID.fromString(uuid))
    }
}
