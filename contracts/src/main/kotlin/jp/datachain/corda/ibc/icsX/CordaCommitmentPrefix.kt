package jp.datachain.corda.ibc.icsX

import jp.datachain.corda.ibc.ics23.CommitmentPath
import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics23.Path
import net.corda.core.transactions.SignedTransaction

data class CordaCommitmentPrefix(val stx: SignedTransaction) : CommitmentPrefix {
    override fun applyPrefix(path: Path): CommitmentPath {
        throw NotImplementedError()
    }
}
