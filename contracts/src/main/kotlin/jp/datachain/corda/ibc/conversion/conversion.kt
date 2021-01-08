package jp.datachain.corda.ibc.conversion

import com.google.protobuf.ByteString
import jp.datachain.corda.ibc.grpc.CordaTypes
import jp.datachain.corda.ibc.grpc.Query
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

fun SecureHash.into(): CordaTypes.SecureHash = CordaTypes.SecureHash.newBuilder().setBytes(ByteString.copyFrom(bytes)).build()
fun CordaTypes.SecureHash.into() = SecureHash.SHA256(bytes.toByteArray())

fun StateRef.into(): CordaTypes.StateRef = CordaTypes.StateRef.newBuilder()
        .setTxhash(txhash.into())
        .setIndex(index)
        .build()
fun CordaTypes.StateRef.into() = StateRef(txhash.into(), index)

fun CordaX500Name.into(): CordaTypes.CordaX500Name = CordaTypes.CordaX500Name.newBuilder()
        .setCommonName(commonName.orEmpty())
        .setCountry(country)
        .setLocality(locality)
        .setOrganisation(organisation)
        .setOrganisationUnit(organisationUnit.orEmpty())
        .setState(state.orEmpty())
        .build()
fun CordaTypes.CordaX500Name.into() = CordaX500Name(
        commonName = if (commonName == "") null else commonName,
        country = country,
        locality = locality,
        organisation = organisation,
        organisationUnit = if (organisationUnit == "") null else organisationUnit,
        state = if (state == "") null else state
)

fun PublicKey.into(): CordaTypes.PublicKey = CordaTypes.PublicKey.newBuilder()
        .setEncoded(ByteString.copyFrom(this.encoded))
        .build()
fun CordaTypes.PublicKey.into() = Crypto.decodePublicKey(encoded.toByteArray())

fun Party.into(): CordaTypes.Party = CordaTypes.Party.newBuilder()
        .setName(name.into())
        .setOwningKey(owningKey.into())
        .build()
fun CordaTypes.Party.into() = Party(name.into(), owningKey.into())

fun Pair<Identifier, Identifier>.into(): Query.Host.PortChannelIdentifier = Query.Host.PortChannelIdentifier.newBuilder()
        .setPortId(first.id)
        .setChannelId(second.id)
        .build()
fun Query.Host.PortChannelIdentifier.into() = Pair(Identifier(portId), Identifier(channelId))

fun Host.into(): Query.Host = Query.Host.newBuilder()
        .addAllParticipants(participants.map{Party(it.nameOrNull()!!, it.owningKey).into()})
        .setBaseId(baseId.into())
        .setNotary(notary.into())
        .addAllClientIds(clientIds.map{it.id})
        .addAllConnIds(connIds.map{it.id})
        .addAllPortChanIds(portChanIds.map{it.into()})
        .setId(id.id)
        .build()
fun Query.Host.into() = Host(
        participants = participantsList.map{it.into()},
        baseId = baseId.into(),
        notary = notary.into(),
        clientIds = clientIdsList.map(::Identifier),
        connIds = connIdsList.map(::Identifier),
        portChanIds = portChanIdsList.map{it.into()})

fun LinkedHashMap<PublicKey, Amount>.into(): Query.Bank.BalanceMapPerDenom = Query.Bank.BalanceMapPerDenom.newBuilder()
        .putAllPubkeyToAmount(this.mapKeys{it.key.encoded.toHex()}.mapValues{it.value.amount.toString()})
        .build()
fun Query.Bank.BalanceMapPerDenom.into() = pubkeyToAmountMap
        .mapKeys{Crypto.decodePublicKey(it.key.parseAsHex())}
        .mapValues{Amount(it.value.toBigInteger())}
        .let{LinkedHashMap<PublicKey, Amount>(it)}

fun LinkedHashMap<Denom, LinkedHashMap<PublicKey, Amount>>.into(): Query.Bank.BalanceMap = Query.Bank.BalanceMap.newBuilder()
        .putAllDenomToMap(this.mapKeys{it.key.denom}.mapValues{it.value.into()})
        .build()
fun Query.Bank.BalanceMap.into() = denomToMapMap
        .mapKeys{Denom(it.key)}
        .mapValues{it.value.into()}
        .let{LinkedHashMap<Denom, LinkedHashMap<PublicKey, Amount>>(it)}

fun Bank.into(): Query.Bank = Query.Bank.newBuilder()
        .addAllParticipants(participants.map{Party(it.nameOrNull()!!, it.owningKey).into()})
        .setBaseId(baseId.into())
        .setAllocated(allocated.into())
        .setLocked(locked.into())
        .setMinted(minted.into())
        .setId(id.id)
        .build()
fun Query.Bank.into() = Bank(
        participants = participantsList.map{it.into()},
        baseId = baseId.into(),
        allocated = allocated.into(),
        locked = locked.into(),
        minted = minted.into())

fun SignatureMetadata.into(): CordaTypes.SignatureMetadata = CordaTypes.SignatureMetadata.newBuilder()
        .setPlatformVersion(platformVersion)
        .setSchemeNumberId(schemeNumberID)
        .build()
fun CordaTypes.SignatureMetadata.into() = SignatureMetadata(
        platformVersion = platformVersion,
        schemeNumberID = schemeNumberId)

fun TransactionSignature.into(): CordaTypes.TransactionSignature = CordaTypes.TransactionSignature.newBuilder()
        .setBytes(ByteString.copyFrom(bytes))
        .setBy(by.into())
        .setSignatureMetadata(signatureMetadata.into())
        .build()
fun CordaTypes.TransactionSignature.into() = TransactionSignature(
        bytes = bytes.toByteArray(),
        by = by.into(),
        signatureMetadata = signatureMetadata.into())

fun SignedTransaction.into(): CordaTypes.SignedTransaction = CordaTypes.SignedTransaction.newBuilder()
        .setTxBits(ByteString.copyFrom(txBits.bytes))
        .addAllSigs(sigs.map{it.into()})
        .build()
fun CordaTypes.SignedTransaction.into() = SignedTransaction(
        txBits = SerializedBytes(txBits.toByteArray()),
        sigs = sigsList.map{it.into()})
