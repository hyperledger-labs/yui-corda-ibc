package jp.datachain.corda.ibc.ics2

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class Height(val height: Long) {
    operator fun compareTo(other: Height) = height.compareTo(other.height)
}
