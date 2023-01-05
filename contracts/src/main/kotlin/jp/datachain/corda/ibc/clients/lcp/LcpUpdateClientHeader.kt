package jp.datachain.corda.ibc.clients.lcp

import com.google.protobuf.Any
import ibc.core.client.v1.Client
import ibc.lightclients.lcp.v1.Lcp
import jp.datachain.corda.ibc.conversion.unpack
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.Header

data class LcpUpdateClientHeader(override val anyHeader: Any): Header {
    val header = anyHeader.unpack<Lcp.UpdateClientHeader>()

    override fun clientType(): ClientType = ClientType.LcpClient

    override fun getHeight(): Client.Height {
        val commitment = StateCommitment.rlpDecode(header.commitment)
        require(commitment != null) { "Invalid commitment: commitment = ${header.commitment}" }
        return commitment!!.height
    }

    override fun validateBasic() {
        TODO("Not yet implemented")
    }
}
