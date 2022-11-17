package jp.datachain.corda.ibc.clients.fabric

import com.google.protobuf.Any
import ibc.core.client.v1.Client
import ibc.lightclients.fabric.v1.Fabric
import jp.datachain.corda.ibc.conversion.unpack
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.Header

data class FabricHeader(override val anyHeader: Any): Header {
    val header = anyHeader.unpack<Fabric.Header>()

    override fun clientType() = ClientType.FabricClient
    override fun getHeight() = Client.Height.newBuilder()
        .setRevisionNumber(0)
        .setRevisionHeight(header.chaincodeHeader.sequence.value)
        .build()!!
    override fun validateBasic() {
        throw NotImplementedError()
    }
}