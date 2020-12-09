package jp.datachain.corda.ibc.clients.corda

import jp.datachain.corda.ibc.ics23.CommitmentProof
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.crypto.TransactionSignature
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
    }
    out.flush()
    return CommitmentProof(rawOut.toByteArray())
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

        val signatureMetadata = SignatureMetadata(
                platformVersion = input.readInt(),
                schemeNumberID = input.readInt()
        )

        sigs.add(TransactionSignature(
                bytes = bytes,
                by = by,
                signatureMetadata = signatureMetadata
        ))
    }

    val stx = SignedTransaction(txBits = txBits, sigs = sigs)
    stx.verifyRequiredSignatures()
    return stx
}