package jp.datachain.corda.ibc.serialization

import ibc.lightclients.lcp.v1.Lcp
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class LcpConsensusStateSerializer: SerializationCustomSerializer<Lcp.ConsensusState, LcpConsensusStateSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Lcp.ConsensusState) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Lcp.ConsensusState.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}
