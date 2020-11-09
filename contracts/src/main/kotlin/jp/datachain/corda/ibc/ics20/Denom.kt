package jp.datachain.corda.ibc.ics20

import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class Denom(val denom: String) {
    fun hasPrefix(portId: Identifier, chanId: Identifier): Boolean {
        val prefix = "${portId.id}/${chanId.id}"
        return denom.length >= prefix.length && denom.substring(0, prefix.length) == prefix
    }

    fun removePrefix(): Denom {
        val firstSlash = denom.findAnyOf(setOf("/"))!!.first
        val secondSlash = denom.findAnyOf(setOf("/"), firstSlash+1)!!.first
        return Denom(denom.substring(secondSlash+1))
    }
}