package jp.datachain.corda.ibc.serialization

import ibc.core.channel.v1.ChannelOuterClass
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class ChannelSerializer: SerializationCustomSerializer<ChannelOuterClass.Channel, ChannelSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: ChannelOuterClass.Channel) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = ChannelOuterClass.Channel.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}