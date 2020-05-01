package jp.datachain.corda.ibc.ics23

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
interface CommitmentPrefix {
    fun applyPrefix(path: Path) : CommitmentPath
}
