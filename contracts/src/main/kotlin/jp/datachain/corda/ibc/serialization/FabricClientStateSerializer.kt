package jp.datachain.corda.ibc.serialization

import ibc.lightclients.fabric.v1.Fabric
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class FabricClientStateSerializer: SerializationCustomSerializer<Fabric.ClientState, FabricClientStateSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Fabric.ClientState) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Fabric.ClientState.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}