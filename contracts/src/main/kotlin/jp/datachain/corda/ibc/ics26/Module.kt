package jp.datachain.corda.ibc.ics26

import net.corda.core.serialization.CordaSerializable
import java.security.PublicKey

@CordaSerializable
abstract class Module {
    abstract val callbacks : ModuleCallbacks
    abstract fun createOutgoingPacket(ctx: Context, signers: Collection<PublicKey>, anyMsg: com.google.protobuf.Any)
    override fun equals(other: Any?) = other?.javaClass == javaClass
    override fun hashCode() = javaClass.name.hashCode()
}