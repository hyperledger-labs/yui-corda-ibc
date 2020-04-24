package jp.datachain.corda.ibc.clients.corda

import jp.datachain.corda.ibc.ics23.CommitmentProof
import net.corda.core.crypto.TransactionSignature
import net.corda.core.transactions.BaseTransaction

data class CordaCommitmentProof(val tx: BaseTransaction, val sig: TransactionSignature) : CommitmentProof