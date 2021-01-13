package jp.datachain.corda.ibc.serialization

import ibc.core.channel.v1.Tx
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class MsgChannelOpenAckSerializer: SerializationCustomSerializer<Tx.MsgChannelOpenAck, MsgChannelOpenAckSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Tx.MsgChannelOpenAck) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Tx.MsgChannelOpenAck.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}