package jp.datachain.corda.ibc.serialization

import ibc.core.client.v1.Tx
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class MsgCreateClientSerializer: SerializationCustomSerializer<Tx.MsgCreateClient, MsgCreateClientSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Tx.MsgCreateClient) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Tx.MsgCreateClient.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}