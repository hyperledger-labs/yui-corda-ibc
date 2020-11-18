package jp.datachain.corda.ibc.grpc

import com.google.common.primitives.SignedBytes
import com.google.protobuf.ByteString
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
import net.corda.core.transactions.CoreTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.parseAsHex
import net.corda.core.utilities.toHex
import java.security.PublicKey
import jp.datachain.corda.ibc.grpc.Host as GrpcHost
import jp.datachain.corda.ibc.grpc.Bank as GrpcBank
import jp.datachain.corda.ibc.grpc.StateRef as GrpcStateRef
import jp.datachain.corda.ibc.grpc.SecureHash as GrpcSecureHash
import jp.datachain.corda.ibc.grpc.Identifier as GrpcIdentifier
import jp.datachain.corda.ibc.grpc.Party as GrpcParty
import jp.datachain.corda.ibc.grpc.CordaX500Name as GrpcCordaX500Name
import jp.datachain.corda.ibc.grpc.PublicKey as GrpcPublicKey
import jp.datachain.corda.ibc.grpc.SignatureMetadata as GrpcSignatureMetadata
import jp.datachain.corda.ibc.grpc.TransactionSignature as GrpcTransactionSignature
import jp.datachain.corda.ibc.grpc.SignedTransaction as GrpcSignedTransaction

fun Identifier.into(): GrpcIdentifier = GrpcIdentifier.newBuilder().setId(id).build()
fun GrpcIdentifier.into() = Identifier(id)

fun SecureHash.into(): GrpcSecureHash = GrpcSecureHash.newBuilder().setBytes(ByteString.copyFrom(bytes)).build()
fun GrpcSecureHash.into() = SecureHash.SHA256(bytes.toByteArray())

fun StateRef.into(): GrpcStateRef = GrpcStateRef.newBuilder()
        .setTxhash(txhash.into())
        .setIndex(index)
        .build()
fun GrpcStateRef.into() = StateRef(txhash.into(), index)

fun CordaX500Name.into(): GrpcCordaX500Name = GrpcCordaX500Name.newBuilder()
        .setCommonName(commonName.orEmpty())
        .setCountry(country)
        .setLocality(locality)
        .setOrganisation(organisation)
        .setOrganisationUnit(organisationUnit.orEmpty())
        .setState(state.orEmpty())
        .build()
fun GrpcCordaX500Name.into() = CordaX500Name(
        commonName = if (commonName == "") null else commonName,
        country = country,
        locality = locality,
        organisation = organisation,
        organisationUnit = if (organisationUnit == "") null else organisationUnit,
        state = if (state == "") null else state
)

fun PublicKey.into(): GrpcPublicKey = GrpcPublicKey.newBuilder()
        .setEncoded(ByteString.copyFrom(this.encoded))
        .build()
fun GrpcPublicKey.into() = Crypto.decodePublicKey(encoded.toByteArray())

fun Party.into(): GrpcParty = GrpcParty.newBuilder()
        .setName(name.into())
        .setOwningKey(owningKey.into())
        .build()
fun GrpcParty.into() = Party(name.into(), owningKey.into())

fun Pair<Identifier, Identifier>.into(): GrpcHost.PortChannelIdentifier = GrpcHost.PortChannelIdentifier.newBuilder()
        .setPortId(first.into())
        .setChannelId(second.into())
        .build()
fun GrpcHost.PortChannelIdentifier.into() = Pair(portId.into(), channelId.into())

fun Host.into(): GrpcHost = GrpcHost.newBuilder()
        .addAllParticipants(participants.map{Party(it.nameOrNull()!!, it.owningKey).into()})
        .setBaseId(baseId.into())
        .setNotary(notary.into())
        .addAllClientIds(clientIds.map{it.into()})
        .addAllConnIds(connIds.map{it.into()})
        .addAllPortChanIds(portChanIds.map{it.into()})
        .setId(id.into())
        .build()
fun GrpcHost.into() = Host(
        participants = participantsList.map{it.into()},
        baseId = baseId.into(),
        notary = notary.into(),
        clientIds = clientIdsList.map{it.into()},
        connIds = connIdsList.map{it.into()},
        portChanIds = portChanIdsList.map{it.into()})

fun LinkedHashMap<PublicKey, Amount>.into(): GrpcBank.BalanceMapPerDenom = GrpcBank.BalanceMapPerDenom.newBuilder()
        .putAllPubkeyToAmount(this.mapKeys{it.key.encoded.toHex()}.mapValues{it.value.toString()})
        .build()
fun GrpcBank.BalanceMapPerDenom.into() = pubkeyToAmountMap
        .mapKeys{Crypto.decodePublicKey(it.key.parseAsHex())}
        .mapValues{Amount(it.value.toBigDecimal())}
        .let{LinkedHashMap<PublicKey, Amount>(it)}

fun LinkedHashMap<Denom, LinkedHashMap<PublicKey, Amount>>.into(): GrpcBank.BalanceMap = GrpcBank.BalanceMap.newBuilder()
        .putAllDenomToMap(this.mapKeys{it.key.denom}.mapValues{it.value.into()})
        .build()
fun GrpcBank.BalanceMap.into() = denomToMapMap
        .mapKeys{Denom(it.key)}
        .mapValues{it.value.into()}
        .let{LinkedHashMap<Denom, LinkedHashMap<PublicKey, Amount>>(it)}

fun Bank.into(): GrpcBank = GrpcBank.newBuilder()
        .addAllParticipants(participants.map{Party(it.nameOrNull()!!, it.owningKey).into()})
        .setBaseId(baseId.into())
        .setAllocated(allocated.into())
        .setLocked(locked.into())
        .setMinted(minted.into())
        .setId(id.into())
        .build()
fun GrpcBank.into() = Bank(
        participants = participantsList.map{it.into()},
        baseId = baseId.into(),
        allocated = allocated.into(),
        locked = locked.into(),
        minted = minted.into())

fun SignatureMetadata.into(): GrpcSignatureMetadata = GrpcSignatureMetadata.newBuilder()
        .setPlatformVersion(platformVersion)
        .setSchemeNumberId(schemeNumberID)
        .build()
fun GrpcSignatureMetadata.into() = SignatureMetadata(
        platformVersion = platformVersion,
        schemeNumberID = schemeNumberId)

fun TransactionSignature.into(): GrpcTransactionSignature = GrpcTransactionSignature.newBuilder()
        .setBytes(ByteString.copyFrom(bytes))
        .setBy(by.into())
        .setSignatureMetadata(signatureMetadata.into())
        .build()
fun GrpcTransactionSignature.into() = TransactionSignature(
        bytes = bytes.toByteArray(),
        by = by.into(),
        signatureMetadata = signatureMetadata.into())

fun SignedTransaction.into(): GrpcSignedTransaction = GrpcSignedTransaction.newBuilder()
        .setTxBits(ByteString.copyFrom(txBits.bytes))
        .addAllSigs(sigs.map{it.into()})
        .build()
fun GrpcSignedTransaction.into() = SignedTransaction(
        txBits = SerializedBytes(txBits.toByteArray()),
        sigs = sigsList.map{it.into()})