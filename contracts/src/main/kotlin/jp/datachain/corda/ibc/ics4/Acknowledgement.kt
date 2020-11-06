package jp.datachain.corda.ibc.ics4

import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.OpaqueBytes

@CordaSerializable
data class Acknowledgement(val data: OpaqueBytes? = null) {
    fun isEmpty() = (data == null)
}
