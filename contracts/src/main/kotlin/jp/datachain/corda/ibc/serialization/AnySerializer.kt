package jp.datachain.corda.ibc.serialization

import com.google.protobuf.Any
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class AnySerializer: SerializationCustomSerializer<Any, AnySerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Any) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Any.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}