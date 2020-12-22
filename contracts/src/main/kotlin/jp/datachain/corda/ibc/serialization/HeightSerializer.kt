package jp.datachain.corda.ibc.serialization

import ibc.core.client.v1.Client
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class HeightSerializer: SerializationCustomSerializer<Client.Height, HeightSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Client.Height) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Client.Height.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}