package jp.datachain.corda.ibc.clients.corda

import com.google.protobuf.ByteString
import jp.datachain.corda.ibc.ics23.CommitmentProof
import net.corda.core.crypto.*
import net.corda.core.serialization.SerializedBytes
import net.corda.core.transactions.CoreTransaction
import net.corda.core.transactions.SignedTransaction
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

fun SignedTransaction.toProof(): CommitmentProof {
    val rawOut = ByteArrayOutputStream()
    val out = DataOutputStream(rawOut)
    out.writeInt(txBits.bytes.size)
    out.write(txBits.bytes)
    out.writeInt(sigs.size)
    sigs.forEach{
        out.writeInt(it.bytes.size)
        out.write(it.bytes)
        val by = it.by.encoded
        out.writeInt(by.size)
        out.write(by)
        out.writeInt(it.signatureMetadata.platformVersion)
        out.writeInt(it.signatureMetadata.schemeNumberID)
        if (it.partialMerkleTree == null) {
            out.writeInt(0)
        } else {
            val root = it.partialMerkleTree!!.root
            when (root) {
                is PartialMerkleTree.PartialTree.IncludedLeaf -> {
                    out.writeInt(1)
                    out.writeInt(root.hash.size)
                    out.write(root.hash.bytes)
                }
                is PartialMerkleTree.PartialTree.Leaf -> {
                    out.writeInt(2)
                    out.writeInt(root.hash.size)
                    out.write(root.hash.bytes)
                }
                is PartialMerkleTree.PartialTree.Node -> {
                    throw NotImplementedError()
                }
            }
        }
    }
    out.flush()
    return CommitmentProof(ByteString.copyFrom(rawOut.toByteArray()))
}

fun CommitmentProof.toSignedTransaction(): SignedTransaction {
    val input = DataInputStream(ByteArrayInputStream(bytes))

    val txBitsBytes = ByteArray(input.readInt())
    input.readFully(txBitsBytes)
    val txBits = SerializedBytes<CoreTransaction>(txBitsBytes)

    val sigs = ArrayList<TransactionSignature>()
    val nSigs = input.readInt()
    repeat(nSigs) {
        val bytes = ByteArray(input.readInt())
        input.readFully(bytes)

        val byBytes = ByteArray(input.readInt())
        input.readFully(byBytes)
        val by = Crypto.decodePublicKey(byBytes)

        val platformVersion = input.readInt()
        val schemeNumberID = input.readInt()
        val signatureMetadata = SignatureMetadata(
                platformVersion = platformVersion,
                schemeNumberID = schemeNumberID
        )

        val partialMerkleTree = when (input.readInt()) {
            0 -> null
            1 -> {
                val hashBytes = ByteArray(input.readInt())
                input.readFully(hashBytes)
                val hash = SecureHash.SHA256(hashBytes)
                PartialMerkleTree(PartialMerkleTree.PartialTree.IncludedLeaf(hash))
            }
            2 -> {
                val hashBytes = ByteArray(input.readInt())
                input.readFully(hashBytes)
                val hash = SecureHash.SHA256(hashBytes)
                PartialMerkleTree(PartialMerkleTree.PartialTree.Leaf(hash))
            }
            else -> throw NotImplementedError()
        }

        sigs.add(TransactionSignature(
                bytes = bytes,
                by = by,
                signatureMetadata = signatureMetadata,
                partialMerkleTree = partialMerkleTree
        ))
    }

    val stx = SignedTransaction(txBits = txBits, sigs = sigs)
    stx.verifyRequiredSignatures()
    return stx
}