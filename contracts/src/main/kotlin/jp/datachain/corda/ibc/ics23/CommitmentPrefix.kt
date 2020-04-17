package jp.datachain.corda.ibc.ics23

interface CommitmentPrefix {
    fun applyPrefix(path: Path) : CommitmentPath
}
