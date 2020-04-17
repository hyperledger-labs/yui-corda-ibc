package jp.datachain.corda.ibc.icsX

import jp.datachain.corda.ibc.ics23.CommitmentProof
import net.corda.core.crypto.TransactionSignature

data class CordaCommitmentProof(val signature: TransactionSignature) : CommitmentProof