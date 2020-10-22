package jp.datachain.corda.ibc.ics26

import net.corda.core.contracts.CommandData
import java.security.PublicKey

interface DatagramHandler: CommandData {
    fun verifySigners(signers: Collection<PublicKey>) {
        // Do nothing by default
    }

    fun execute(ctx: Context)
}