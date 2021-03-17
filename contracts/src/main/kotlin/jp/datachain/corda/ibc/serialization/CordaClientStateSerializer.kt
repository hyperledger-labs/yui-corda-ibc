package jp.datachain.corda.ibc.serialization

import ibc.lightclients.corda.v1.Corda
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class CordaClientStateSerializer: SerializationCustomSerializer<Corda.ClientState, CordaClientStateSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Corda.ClientState) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Corda.ClientState.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}