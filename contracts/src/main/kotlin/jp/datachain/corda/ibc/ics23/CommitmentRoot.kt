package jp.datachain.corda.ibc.ics23

interface CommitmentRoot {
    fun verifyMembership(proof: CommitmentProof, path: CommitmentPath, value: Value) : Boolean
    fun verifyNonMembership(proof: CommitmentProof, path: CommitmentPath) : Boolean

    fun batchVerifyMembership(proof: CommitmentProof, items: Map<CommitmentPath, Value>) {
        items.all{verifyMembership(proof, it.key, it.value)}
    }
    fun batchVerifyNonMembership(proof: CommitmentProof, items: Collection<CommitmentPath>) {
        items.all{verifyNonMembership(proof, it)}
    }
}
