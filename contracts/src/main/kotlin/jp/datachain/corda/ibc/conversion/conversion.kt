package jp.datachain.corda.ibc.conversion

import com.google.protobuf.ByteString
import jp.datachain.corda.ibc.grpc.Corda
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.crypto.TransactionSignature
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.serialization.SerializedBytes
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.parseAsHex
import net.corda.core.utilities.toHex
import java.security.PublicKey

fun SecureHash.into(): Corda.SecureHash = Corda.SecureHash.newBuilder().setBytes(ByteString.copyFrom(bytes)).build()
fun Corda.SecureHash.into() = SecureHash.SHA256(bytes.toByteArray())

fun StateRef.into(): Corda.StateRef = Corda.StateRef.newBuilder()
        .setTxhash(txhash.into())
        .setIndex(index)
        .build()
fun Corda.StateRef.into() = StateRef(txhash.into(), index)

fun CordaX500Name.into(): Corda.CordaX500Name = Corda.CordaX500Name.newBuilder()
        .setCommonName(commonName.orEmpty())
        .setCountry(country)
        .setLocality(locality)
        .setOrganisation(organisation)
        .setOrganisationUnit(organisationUnit.orEmpty())
        .setState(state.orEmpty())
        .build()
fun Corda.CordaX500Name.into() = CordaX500Name(
        commonName = if (commonName == "") null else commonName,
        country = country,
        locality = locality,
        organisation = organisation,
        organisationUnit = if (organisationUnit == "") null else organisationUnit,
        state = if (state == "") null else state
)

fun PublicKey.into(): Corda.PublicKey = Corda.PublicKey.newBuilder()
        .setEncoded(ByteString.copyFrom(this.encoded))
        .build()
fun Corda.PublicKey.into() = Crypto.decodePublicKey(encoded.toByteArray())

fun Party.into(): Corda.Party = Corda.Party.newBuilder()
        .setName(name.into())
        .setOwningKey(owningKey.into())
        .build()
fun Corda.Party.into() = Party(name.into(), owningKey.into())

fun Pair<Identifier, Identifier>.into(): Corda.Host.PortChannelIdentifier = Corda.Host.PortChannelIdentifier.newBuilder()
        .setPortId(first.id)
        .setChannelId(second.id)
        .build()
fun Corda.Host.PortChannelIdentifier.into() = Pair(Identifier(portId), Identifier(channelId))

fun Host.into(): Corda.Host = Corda.Host.newBuilder()
        .addAllParticipants(participants.map{Party(it.nameOrNull()!!, it.owningKey).into()})
        .setBaseId(baseId.into())
        .setNotary(notary.into())
        .addAllClientIds(clientIds.map{it.id})
        .addAllConnIds(connIds.map{it.id})
        .addAllPortChanIds(portChanIds.map{it.into()})
        .setId(id.id)
        .build()
fun Corda.Host.into() = Host(
        participants = participantsList.map{it.into()},
        baseId = baseId.into(),
        notary = notary.into(),
        clientIds = clientIdsList.map(::Identifier),
        connIds = connIdsList.map(::Identifier),
        portChanIds = portChanIdsList.map{it.into()})

fun LinkedHashMap<PublicKey, Amount>.into(): Corda.Bank.BalanceMapPerDenom = Corda.Bank.BalanceMapPerDenom.newBuilder()
        .putAllPubkeyToAmount(this.mapKeys{it.key.encoded.toHex()}.mapValues{it.value.amount.toString()})
        .build()
fun Corda.Bank.BalanceMapPerDenom.into() = pubkeyToAmountMap
        .mapKeys{Crypto.decodePublicKey(it.key.parseAsHex())}
        .mapValues{Amount(it.value.toBigInteger())}
        .let{LinkedHashMap<PublicKey, Amount>(it)}

fun LinkedHashMap<Denom, LinkedHashMap<PublicKey, Amount>>.into(): Corda.Bank.BalanceMap = Corda.Bank.BalanceMap.newBuilder()
        .putAllDenomToMap(this.mapKeys{it.key.denom}.mapValues{it.value.into()})
        .build()
fun Corda.Bank.BalanceMap.into() = denomToMapMap
        .mapKeys{Denom(it.key)}
        .mapValues{it.value.into()}
        .let{LinkedHashMap<Denom, LinkedHashMap<PublicKey, Amount>>(it)}

fun Bank.into(): Corda.Bank = Corda.Bank.newBuilder()
        .addAllParticipants(participants.map{Party(it.nameOrNull()!!, it.owningKey).into()})
        .setBaseId(baseId.into())
        .setAllocated(allocated.into())
        .setLocked(locked.into())
        .setMinted(minted.into())
        .setId(id.id)
        .build()
fun Corda.Bank.into() = Bank(
        participants = participantsList.map{it.into()},
        baseId = baseId.into(),
        allocated = allocated.into(),
        locked = locked.into(),
        minted = minted.into())

fun SignatureMetadata.into(): Corda.SignatureMetadata = Corda.SignatureMetadata.newBuilder()
        .setPlatformVersion(platformVersion)
        .setSchemeNumberId(schemeNumberID)
        .build()
fun Corda.SignatureMetadata.into() = SignatureMetadata(
        platformVersion = platformVersion,
        schemeNumberID = schemeNumberId)

fun TransactionSignature.into(): Corda.TransactionSignature = Corda.TransactionSignature.newBuilder()
        .setBytes(ByteString.copyFrom(bytes))
        .setBy(by.into())
        .setSignatureMetadata(signatureMetadata.into())
        .build()
fun Corda.TransactionSignature.into() = TransactionSignature(
        bytes = bytes.toByteArray(),
        by = by.into(),
        signatureMetadata = signatureMetadata.into())

fun SignedTransaction.into(): Corda.SignedTransaction = Corda.SignedTransaction.newBuilder()
        .setTxBits(ByteString.copyFrom(txBits.bytes))
        .addAllSigs(sigs.map{it.into()})
        .build()
fun Corda.SignedTransaction.into() = SignedTransaction(
        txBits = SerializedBytes(txBits.toByteArray()),
        sigs = sigsList.map{it.into()})
