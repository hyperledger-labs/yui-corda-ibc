package jp.datachain.corda.ibc.serialization

import ibc.core.channel.v1.Tx
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class MsgRecvPacketSerializer: SerializationCustomSerializer<Tx.MsgRecvPacket, MsgRecvPacketSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Tx.MsgRecvPacket) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Tx.MsgRecvPacket.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}