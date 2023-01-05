package jp.datachain.corda.ibc.clients.lcp

import com.google.protobuf.Any
import ibc.core.client.v1.Client
import ibc.lightclients.lcp.v1.Lcp
import jp.datachain.corda.ibc.conversion.unpack
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.Header

data class LcpRegisterEnclaveKeyHeader(override val anyHeader: Any): Header {
    val header = anyHeader.unpack<Lcp.RegisterEnclaveKeyHeader>()

    override fun clientType(): ClientType = ClientType.LcpClient

    override fun getHeight(): Client.Height =
        Client.Height.newBuilder()
            .setRevisionNumber(0)
            .setRevisionHeight(0)
            .build()

    override fun validateBasic() {
        /* no-op */
        return
    }
}
