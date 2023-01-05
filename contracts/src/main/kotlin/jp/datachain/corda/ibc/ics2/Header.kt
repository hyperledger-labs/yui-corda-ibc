package jp.datachain.corda.ibc.ics2

import com.google.protobuf.Any
import ibc.core.client.v1.Client
import net.corda.core.serialization.CordaSerializable

// TODO: これを満たしたHeaderがLcpHeaderが必要
@CordaSerializable
interface Header {
    val anyHeader: Any

    fun clientType(): ClientType
    fun getHeight(): Client.Height
    fun validateBasic()
}
