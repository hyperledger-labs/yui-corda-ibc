package jp.datachain.corda.ibc.ics20

import ibc.applications.transfer.v1.Transfer
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.Issued
import net.corda.core.contracts.PartyAndReference
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.hexToByteArray
import net.corda.core.utilities.toHex
import java.util.*
import javax.security.auth.x500.X500Principal

@CordaSerializable
data class Denom(val denomTrace: Transfer.DenomTrace) {

    companion object {
        private fun encodeToken(token: Issued<Currency>) : String {
            val party = token.issuer.party as Party
            val partyName = party.name.x500Principal.encoded.toHex()
            val partyKey = party.owningKey.encoded.toHex()
            val reference = token.issuer.reference.bytes.toHex()
            val currency = token.product.currencyCode
            return "$partyName:$partyKey:$reference:$currency"
        }

        private fun decodeToken(denom: String) : Issued<Currency> {
            val words = denom.split(':')
            require(words.size == 4)

            // decode party
            val partyName = CordaX500Name.build(X500Principal(words[0].hexToByteArray()))
            val partyKey = Crypto.decodePublicKey(words[1].hexToByteArray())
            val party = Party(partyName, partyKey)

            // decode reference
            val reference = OpaqueBytes(words[2].hexToByteArray())

            // issuer = party + reference
            val issuer = PartyAndReference(party, reference)

            // decode currency
            val currency = Currency.getInstance(words[3])!!

            return Issued(issuer, currency)
        }

        fun fromToken(token: Issued<Currency>) = Denom(
                Transfer.DenomTrace.newBuilder()
                        .setBaseDenom(encodeToken(token))
                        .build())

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

    override fun toString() = if (denomTrace.path.isEmpty()) {
        denomTrace.baseDenom
    } else {
        "${denomTrace.path}/${denomTrace.baseDenom}"
    }

    fun toToken() : Issued<Currency> {
        require(denomTrace.path.isEmpty())
        return decodeToken(this.denomTrace.baseDenom)
    }

    fun toIbcDenom() : String {
        val hash = SecureHash.sha256(toString()).bytes.toHex()
        return "ibc/$hash"
    }
}