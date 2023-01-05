package jp.datachain.corda.ibc.serialization

import ibc.lightclients.lcp.v1.Lcp
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class LcpClientStateSerializer: SerializationCustomSerializer<Lcp.ClientState, LcpClientStateSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Lcp.ClientState) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Lcp.ClientState.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}
