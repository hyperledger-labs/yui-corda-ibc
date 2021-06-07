package jp.datachain.corda.ibc.serialization

import ibc.applications.transfer.v1.Transfer
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class DenomTraceSerializer: SerializationCustomSerializer<Transfer.DenomTrace, DenomTraceSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Transfer.DenomTrace) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Transfer.DenomTrace.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}