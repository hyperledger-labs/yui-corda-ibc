package jp.datachain.corda.ibc.serialization

import ibc.core.channel.v1.Tx
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class MsgChannelOpenInitSerializer: SerializationCustomSerializer<Tx.MsgChannelOpenInit, MsgChannelOpenInitSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Tx.MsgChannelOpenInit) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Tx.MsgChannelOpenInit.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}