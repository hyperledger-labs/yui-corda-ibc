package jp.datachain.corda.ibc.serialization

import ibc.core.channel.v1.ChannelOuterClass
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class AcknowledgementSerializer: SerializationCustomSerializer<ChannelOuterClass.Acknowledgement, AcknowledgementSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: ChannelOuterClass.Acknowledgement) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = ChannelOuterClass.Acknowledgement.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}