package jp.datachain.corda.ibc.ics23

interface CommitmentState {
    fun calculateRoot() : CommitmentRoot

    fun set(path: Path, value: Value) : CommitmentState
    fun remove(path: Path) : CommitmentState

    fun createMembershipProof(path: CommitmentPath, value: Value) : CommitmentProof
    fun createNonMembershipProof(path: CommitmentPath) : CommitmentProof
}
