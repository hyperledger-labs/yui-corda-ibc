package jp.datachain.corda.ibc.clients.corda

import jp.datachain.corda.ibc.ics23.CommitmentPath
import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics23.Path

class CordaCommitmentPrefix : CommitmentPrefix {
    override fun equals(other: Any?) = other?.javaClass == javaClass
    override fun hashCode() = javaClass.name.hashCode()

    override fun applyPrefix(path: Path): CommitmentPath = throw NotImplementedError()
}