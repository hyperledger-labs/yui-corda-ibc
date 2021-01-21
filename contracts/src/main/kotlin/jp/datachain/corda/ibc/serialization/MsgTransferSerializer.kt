package jp.datachain.corda.ibc.serialization

import ibc.applications.transfer.v1.Tx
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class MsgTransferSerializer: SerializationCustomSerializer<Tx.MsgTransfer, MsgTransferSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Tx.MsgTransfer) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Tx.MsgTransfer.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}