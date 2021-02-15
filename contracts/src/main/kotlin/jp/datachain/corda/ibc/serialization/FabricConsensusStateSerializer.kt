package jp.datachain.corda.ibc.serialization

import ibc.lightclients.fabric.v1.Fabric
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class FabricConsensusStateSerializer: SerializationCustomSerializer<Fabric.ConsensusState, FabricConsensusStateSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Fabric.ConsensusState) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Fabric.ConsensusState.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}