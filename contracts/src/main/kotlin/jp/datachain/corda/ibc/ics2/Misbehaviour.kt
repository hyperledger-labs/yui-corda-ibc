package jp.datachain.corda.ibc.ics2

import com.google.protobuf.Any
import ibc.core.client.v1.Client
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
interface Misbehaviour {
    val anyMisbehaviour: Any

    fun clientType(): ClientType
    fun getClientID(): Identifier
    fun validateBasic()

    // Height at which the infraction occurred
    fun getHeight(): Client.Height
}
