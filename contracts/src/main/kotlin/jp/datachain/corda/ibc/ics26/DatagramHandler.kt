package jp.datachain.corda.ibc.ics26

import net.corda.core.contracts.CommandData
import java.security.PublicKey

interface DatagramHandler: CommandData {
    fun execute(ctx: Context, signers: Collection<PublicKey>)
}