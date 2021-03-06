package jp.datachain.corda.ibc.conversion

import com.google.protobuf.ByteString
import ibc.lightclients.corda.v1.BankProto
import ibc.lightclients.corda.v1.CashBankProto
import ibc.lightclients.corda.v1.CordaTypes
import ibc.lightclients.corda.v1.HostProto
import jp.datachain.corda.ibc.ics20.Address
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics20cash.CashBank
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

fun Host.into(): HostProto.Host = HostProto.Host.newBuilder()
        .addAllParticipants(participants.map{Party(it.nameOrNull()!!, it.owningKey).into()})
        .setBaseId(baseId.into())
        .setNotary(notary.into())
        .setNextClientSequence(nextClientSequence)
        .setNextConnectionSequence(nextConnectionSequence)
        .setNextChannelSequence(nextChannelSequence)
        .addAllBankIds(bankIds.map{it.id})
        .build()
fun HostProto.Host.into() = Host(
        participants = participantsList.map{it.into()},
        baseId = baseId.into(),
        notary = notary.into(),
        nextClientSequence = nextClientSequence,
        nextConnectionSequence = nextConnectionSequence,
        nextChannelSequence = nextChannelSequence,
        bankIds = bankIdsList.map(::Identifier)
)

fun MutableMap<Address, Amount>.into(): BankProto.Bank.BalanceMapPerDenom = BankProto.Bank.BalanceMapPerDenom.newBuilder()
        .putAllPubkeyToAmount(this
                .mapKeys{it.key.toBech32()}
                .mapValues{it.value.toString()})
        .build()
fun BankProto.Bank.BalanceMapPerDenom.into() = pubkeyToAmountMap
        .mapKeys{Address.fromBech32(it.key)}
        .mapValues{Amount.fromString(it.value)}
        .toMutableMap()

fun MutableMap<Denom, MutableMap<Address, Amount>>.into(): BankProto.Bank.BalanceMap = BankProto.Bank.BalanceMap.newBuilder()
        .putAllDenomToMap(this
                .mapKeys{it.key.toString()}
                .mapValues{it.value.into()})
        .build()
fun BankProto.Bank.BalanceMap.into() = denomToMapMap
        .mapKeys{Denom.fromString(it.key)}
        .mapValues{it.value.into()}
        .toMutableMap()

fun MutableMap<String, Denom>.into(): BankProto.Bank.IbcDenomMap = BankProto.Bank.IbcDenomMap.newBuilder()
        .putAllIbcDenomToDenom(this
                .mapKeys{it.key}
                .mapValues{it.value.toString()})
        .build()
fun BankProto.Bank.IbcDenomMap.into() = ibcDenomToDenomMap
        .mapValues{Denom.fromString(it.value)}
        .toMutableMap()

fun Bank.into(): BankProto.Bank = BankProto.Bank.newBuilder()
        .addAllParticipants(participants.map{Party(it.nameOrNull()!!, it.owningKey).into()})
        .setBaseId(baseId.into())
        .setAllocated(allocated.into())
        .setLocked(locked.into())
        .setMinted(minted.into())
        .setDenoms(denoms.into())
        .build()
fun BankProto.Bank.into() = Bank(
        participants = participantsList.map{it.into()},
        baseId = baseId.into(),
        allocated = allocated.into(),
        locked = locked.into(),
        minted = minted.into(),
        denoms = denoms.into())

fun Map<Denom, Amount>.into(): CashBankProto.CashBank.SupplyMap = CashBankProto.CashBank.SupplyMap.newBuilder()
        .putAllDenomToAmount(this
                .mapKeys{it.key.toString()}
                .mapValues{it.value.toString()})
        .build()
fun CashBankProto.CashBank.SupplyMap.into(): Map<Denom, Amount> = denomToAmountMap
        .mapKeys{Denom.fromString(it.key)}
        .mapValues{Amount.fromString(it.value)}

fun Map<String, Denom>.into(): CashBankProto.CashBank.IbcDenomMap = CashBankProto.CashBank.IbcDenomMap.newBuilder()
        .putAllIbcDenomToDenom(this
                .mapKeys{it.key}
                .mapValues{it.value.toString()})
        .build()
fun CashBankProto.CashBank.IbcDenomMap.into(): Map<String, Denom> = ibcDenomToDenomMap
        .mapValues{Denom.fromString(it.value)}
        .toMap()

fun CashBank.into(): CashBankProto.CashBank = CashBankProto.CashBank.newBuilder()
        .addAllParticipants(participants.map{Party(it.nameOrNull()!!, it.owningKey).into()})
        .setBaseId(baseId.into())
        .setOwner(owner.into())
        .setSupply(supply.into())
        .setDenoms(denoms.into())
        .build()