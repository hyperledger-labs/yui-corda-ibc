package jp.datachain.corda.ibc.serialization

import ibc.core.connection.v1.Connection
import net.corda.core.serialization.SerializationCustomSerializer
import net.corda.core.utilities.OpaqueBytes

class ConnectionEndSerializer: SerializationCustomSerializer<Connection.ConnectionEnd, ConnectionEndSerializer.Proxy> {
    data class Proxy(val serialized: OpaqueBytes)

    override fun toProxy(obj: Connection.ConnectionEnd) = Proxy(OpaqueBytes(obj.toByteArray() + 0))
    override fun fromProxy(proxy: Proxy) = Connection.ConnectionEnd.parseFrom(proxy.serialized.bytes.let { it.copyOf(it.size - 1) })!!
}