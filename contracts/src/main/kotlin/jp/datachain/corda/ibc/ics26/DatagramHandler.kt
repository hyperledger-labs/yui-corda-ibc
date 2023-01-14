package jp.datachain.corda.ibc.ics26

import net.corda.core.serialization.CordaSerializable
import java.security.PublicKey

@CordaSerializable
interface DatagramHandler {
    fun execute(ctx: Context, signers: Collection<PublicKey>)
}