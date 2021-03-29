package jp.datachain.corda.ibc.conversion

import com.google.protobuf.ByteString
import ibc.lightclients.corda.v1.CordaTypes
import ibc.lightclients.corda.v1.HostAndBank
import jp.datachain.corda.ibc.ics20.Address
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
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

fun Pair<Identifier, Identifier>.into(): HostAndBank.Host.PortChannelIdentifier = HostAndBank.Host.PortChannelIdentifier.newBuilder()
        .setPortId(first.id)
        .setChannelId(second.id)
        .build()
fun HostAndBank.Host.PortChannelIdentifier.into() = Pair(Identifier(portId), Identifier(channelId))

fun Host.into(): HostAndBank.Host = HostAndBank.Host.newBuilder()
        .addAllParticipants(participants.map{Party(it.nameOrNull()!!, it.owningKey).into()})
        .setBaseId(baseId.into())
        .setNotary(notary.into())
        .addAllClientIds(clientIds.map{it.id})
        .addAllConnIds(connIds.map{it.id})
        .addAllPortChanIds(portChanIds.map{it.into()})
        .setId(id.id)
        .build()
fun HostAndBank.Host.into() = Host(
        participants = participantsList.map{it.into()},
        baseId = baseId.into(),
        notary = notary.into(),
        clientIds = clientIdsList.map(::Identifier),
        connIds = connIdsList.map(::Identifier),
        portChanIds = portChanIdsList.map{it.into()})

fun MutableMap<Address, Amount>.into(): HostAndBank.Bank.BalanceMapPerDenom = HostAndBank.Bank.BalanceMapPerDenom.newBuilder()
        .putAllPubkeyToAmount(this.mapKeys{it.key.address}.mapValues{it.value.amount.toString()})
        .build()
fun HostAndBank.Bank.BalanceMapPerDenom.into() = pubkeyToAmountMap
        .mapKeys{Address(it.key)}
        .mapValues{Amount(it.value.toBigInteger())}
        .toMutableMap()

fun MutableMap<Denom, MutableMap<Address, Amount>>.into(): HostAndBank.Bank.BalanceMap = HostAndBank.Bank.BalanceMap.newBuilder()
        .putAllDenomToMap(this.mapKeys{it.key.denom}.mapValues{it.value.into()})
        .build()
fun HostAndBank.Bank.BalanceMap.into() = denomToMapMap
        .mapKeys{Denom(it.key)}
        .mapValues{it.value.into()}
        .toMutableMap()

fun MutableMap<Denom, Denom>.into(): HostAndBank.Bank.IbcDenomMap = HostAndBank.Bank.IbcDenomMap.newBuilder()
        .putAllIbcDenomToDenom(this.mapKeys{it.key.denom}.mapValues{it.value.denom})
        .build()
fun HostAndBank.Bank.IbcDenomMap.into() = ibcDenomToDenomMap
        .mapKeys{Denom(it.key)}
        .mapValues{Denom(it.value)}
        .toMutableMap()

fun Bank.into(): HostAndBank.Bank = HostAndBank.Bank.newBuilder()
        .addAllParticipants(participants.map{Party(it.nameOrNull()!!, it.owningKey).into()})
        .setBaseId(baseId.into())
        .setAllocated(allocated.into())
        .setLocked(locked.into())
        .setMinted(minted.into())
        .setDenoms(denoms.into())
        .setId(id.id)
        .build()
fun HostAndBank.Bank.into() = Bank(
        participants = participantsList.map{it.into()},
        baseId = baseId.into(),
        allocated = allocated.into(),
        locked = locked.into(),
        minted = minted.into(),
        denoms = denoms.into())