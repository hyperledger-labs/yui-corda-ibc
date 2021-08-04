package jp.datachain.corda.ibc.ics20

import ibc.applications.transfer.v1.Transfer
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.hexToByteArray
import net.corda.core.utilities.toHex
import java.security.PublicKey
import java.util.*

@CordaSerializable
data class Denom(val denomTrace: Transfer.DenomTrace) {

    companion object {
        // INFO: denomination string must be alphanumerical and start from an alphabetical character
        private fun encodeIssuedCurrency(issuerKey: PublicKey, currency: Currency) : String {
            val issuerKeyString = issuerKey.encoded.toHex()
            val currencyString = currency.currencyCode
            return "$issuerKeyString$currencyString"
        }

        private fun decodeIssuedCurrency(issuedCurrency: String) : Pair<PublicKey, Currency> {
            val issuerKeyPart = issuedCurrency.slice(0 until issuedCurrency.length - 3)
            val currencyPart = issuedCurrency.slice(issuedCurrency.length - 3 until issuedCurrency.length)
            val issuerKey = Crypto.decodePublicKey(issuerKeyPart.hexToByteArray())
            val currency = Currency.getInstance(currencyPart)!!
            return Pair(issuerKey, currency)
        }

        fun fromIssuedCurrency(issuerKey: PublicKey, currency: Currency) = fromString(encodeIssuedCurrency(issuerKey, currency))

        fun fromString(denom: String) = Denom(
                denom.split('/').let {
                    Transfer.DenomTrace.newBuilder()
                            .setPath(it.dropLast(1).joinToString("/"))
                            .setBaseDenom(it.last())
                            .build()
                })
    }

    fun addPath(portId: Identifier, chanId: Identifier) = Denom(denomTrace.toBuilder()
            .setPath(denomTrace.path.addPath(portId, chanId))
            .build())

    fun hasPrefix(portId: Identifier, chanId: Identifier)
            = denomTrace.path.hasPrefixes(portId.id, chanId.id)

    fun removePrefix() = Denom(denomTrace.toBuilder()
            .setPath(denomTrace.path.removePrefix())
            .build())

    fun isVoucher() = denomTrace.path.isNotEmpty()

    val issuerKey: PublicKey get() {
        require(!isVoucher())
        val (issuerKey, _) = decodeIssuedCurrency(denomTrace.baseDenom)
        return issuerKey
    }

    val currency: Currency get() {
        require(!isVoucher())
        val (_, currency) = decodeIssuedCurrency(denomTrace.baseDenom)
        return currency
    }

    override fun toString() = if (isVoucher()) {
        "${denomTrace.path}/${denomTrace.baseDenom}"
    } else {
        denomTrace.baseDenom!!
    }

    fun toIbcDenom() : String {
        val hash = SecureHash.sha256(toString()).bytes.toHex()
        return "ibc/$hash"
    }
}