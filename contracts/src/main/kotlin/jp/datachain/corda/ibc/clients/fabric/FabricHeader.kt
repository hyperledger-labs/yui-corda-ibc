package jp.datachain.corda.ibc.clients.fabric

import com.google.protobuf.Any
import ibc.core.client.v1.Client
import ibc.lightclients.fabric.v1.Fabric
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.Header

data class FabricHeader(val fabricHeader: Fabric.Header): Header {
    override val header get() = Any.pack(fabricHeader, "")!!

    override fun clientType() = ClientType.FabricClient
    override fun getHeight() = Client.Height.newBuilder()
        .setRevisionNumber(0)
        .setRevisionHeight(fabricHeader.chaincodeHeader.sequence.value)
        .build()!!
    override fun validateBasic() {
        throw NotImplementedError()
    }
}