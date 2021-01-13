package jp.datachain.corda.ibc.serialization

import ibc.core.client.v1.Client
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class MsgCreateClientSerializer: SerializationCustomSerializer<Client.MsgCreateClient, MsgCreateClientSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Client.MsgCreateClient) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Client.MsgCreateClient.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}