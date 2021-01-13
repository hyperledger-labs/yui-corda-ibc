package jp.datachain.corda.ibc.serialization

import ibc.core.connection.v1.Tx
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class MsgConnectionOpenTrySerializer: SerializationCustomSerializer<Tx.MsgConnectionOpenTry, MsgConnectionOpenTrySerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Tx.MsgConnectionOpenTry) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Tx.MsgConnectionOpenTry.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}