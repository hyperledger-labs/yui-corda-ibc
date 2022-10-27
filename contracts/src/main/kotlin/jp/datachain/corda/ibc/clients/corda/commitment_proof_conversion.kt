package jp.datachain.corda.ibc.clients.corda

import com.google.protobuf.ByteString
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.states.IbcState
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.*
import net.corda.core.identity.Party
import net.corda.core.serialization.*
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.SignedTransaction
import java.util.function.Predicate

private fun createSerializedSignableDataList(stx: SignedTransaction): List<SerializedBytes<SignableData>> = stx.sigs
        .map { sig ->
            val partialMerkleTree = sig.partialMerkleTree
            val txId = if (partialMerkleTree == null) {
                stx.id
            } else {
                val usedHashes = mutableListOf<SecureHash>()
                val root = PartialMerkleTree.rootAndUsedHashes(partialMerkleTree.root, usedHashes)
                if (!usedHashes.contains(stx.id.reHash())) {
                    throw IllegalArgumentException("stx.id isn't included in partialMerkleTree.")
                }
                root
            }
            SignableData(txId, sig.signatureMetadata)
        }
        .map { it.serialize() }

fun SignedTransaction.toProof(): CommitmentProof {
    val tx: FilteredTransaction = buildFilteredTransaction(Predicate { it is Party || (it is TransactionState<*> && it.data is IbcState) })
    tx.verify()
    val stx = SignedTransaction(tx, sigs)
    val proof = Pair(stx, createSerializedSignableDataList(stx))
    return CommitmentProof(ByteString.copyFrom(proof.serialize().bytes))
}

fun CommitmentProof.toSignedTransaction(): SignedTransaction {
    val proof = this.bytes.deserialize<Pair<SignedTransaction, List<SerializedBytes<SignableData>>>>()
    val stx = proof.first
    (stx.coreTransaction as FilteredTransaction).verify()
    return proof.first
}