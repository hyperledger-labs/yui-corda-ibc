package jp.datachain.corda.ibc.serialization

import jp.datachain.corda.ibc.grpc.Corda
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class CordaConsensusStateSerializer: SerializationCustomSerializer<Corda.ConsensusState, CordaConsensusStateSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Corda.ConsensusState) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Corda.ConsensusState.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}