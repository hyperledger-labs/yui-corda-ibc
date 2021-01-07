package jp.datachain.corda.ibc.ics2

import com.google.protobuf.Any
import ibc.core.client.v1.Client
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
interface Header {
    val header: Any

    fun clientType(): ClientType
    fun getHeight(): Client.Height
    fun validateBasic()
}
