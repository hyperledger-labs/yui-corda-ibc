package jp.datachain.corda.ibc.conversion

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.Message
import ibc.lightclients.corda.v1.BankProto
import ibc.lightclients.corda.v1.CashBankProto
import ibc.lightclients.corda.v1.CordaTypes
import ibc.lightclients.corda.v1.HostProto
import jp.datachain.corda.ibc.ics2.ClientStateFactory
import jp.datachain.corda.ibc.ics20.Address
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics20cash.CashBank
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Module
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import java.security.PublicKey

fun Message.pack() = Any.pack(this, "")!!
inline fun<reified T: Message> Any.unpack() = this.unpack(T::class.java)!!

fun SecureHash.toProto(): CordaTypes.SecureHash = CordaTypes.SecureHash.newBuilder().setBytes(ByteString.copyFrom(bytes)).build()
fun CordaTypes.SecureHash.toCorda() = SecureHash.SHA256(bytes.toByteArray())

fun StateRef.toProto(): CordaTypes.StateRef = CordaTypes.StateRef.newBuilder()
        .setTxhash(txhash.toProto())
        .setIndex(index)
        .build()
fun CordaTypes.StateRef.toCorda() = StateRef(txhash.toCorda(), index)

fun CordaX500Name.toProto(): CordaTypes.CordaX500Name = CordaTypes.CordaX500Name.newBuilder()
        .setCommonName(commonName.orEmpty())
        .setCountry(country)
        .setLocality(locality)
        .setOrganisation(organisation)
        .setOrganisationUnit(organisationUnit.orEmpty())
        .setState(state.orEmpty())
        .build()
fun CordaTypes.CordaX500Name.toCorda() = CordaX500Name(
        commonName = if (commonName == "") null else commonName,
        country = country,
        locality = locality,
        organisation = organisation,
        organisationUnit = if (organisationUnit == "") null else organisationUnit,
        state = if (state == "") null else state
)

fun PublicKey.toProto(): CordaTypes.PublicKey = CordaTypes.PublicKey.newBuilder()
        .setEncoded(ByteString.copyFrom(this.encoded))
        .build()
fun CordaTypes.PublicKey.toCorda() = Crypto.decodePublicKey(encoded.toByteArray())

fun Party.toProto(): CordaTypes.Party = CordaTypes.Party.newBuilder()
        .setName(name.toProto())
        .setOwningKey(owningKey.toProto())
        .build()
fun CordaTypes.Party.toCorda() = Party(name.toCorda(), owningKey.toCorda())

fun Host.toProto(): HostProto.Host = HostProto.Host.newBuilder()
        .addAllParticipants(participants.map{Party(it.nameOrNull()!!, it.owningKey).toProto()})
        .setBaseId(baseId.toProto())
        .setNotary(notary.toProto())
        .setNextClientSequence(nextClientSequence)
        .setNextConnectionSequence(nextConnectionSequence)
        .setNextChannelSequence(nextChannelSequence)
        .putAllModuleNames(modules.mapKeys{it.key.id}.mapValues{it.value::class.qualifiedName})
        .putAllClientStateFactoryNames(clientStateFactories.mapValues{it.value::class.qualifiedName})
        .addAllBankIds(bankIds.map{it.id})
        .build()
fun HostProto.Host.toCorda() = Host(
        participants = participantsList.map{it.toCorda()},
        baseId = baseId.toCorda(),
        notary = notary.toCorda(),
        nextClientSequence = nextClientSequence,
        nextConnectionSequence = nextConnectionSequence,
        nextChannelSequence = nextChannelSequence,
        modules = moduleNamesMap.mapKeys{Identifier(it.key)}.mapValues{Host.createInstance<Module>(it.value)},
        clientStateFactories = clientStateFactoryNamesMap.mapValues{Host.createInstance<ClientStateFactory>(it.value)},
        bankIds = bankIdsList.map(::Identifier)
)

fun MutableMap<Address, Amount>.toProto(): BankProto.Bank.BalanceMapPerDenom = BankProto.Bank.BalanceMapPerDenom.newBuilder()
        .putAllPubkeyToAmount(this
                .mapKeys{it.key.toBech32()}
                .mapValues{it.value.toString()})
        .build()
fun BankProto.Bank.BalanceMapPerDenom.toCorda() = pubkeyToAmountMap
        .mapKeys{Address.fromBech32(it.key)}
        .mapValues{Amount.fromString(it.value)}
        .toMutableMap()

fun MutableMap<Denom, MutableMap<Address, Amount>>.toProto(): BankProto.Bank.BalanceMap = BankProto.Bank.BalanceMap.newBuilder()
        .putAllDenomToMap(this
                .mapKeys{it.key.toString()}
                .mapValues{it.value.toProto()})
        .build()
fun BankProto.Bank.BalanceMap.toCorda() = denomToMapMap
        .mapKeys{Denom.fromString(it.key)}
        .mapValues{it.value.toCorda()}
        .toMutableMap()

fun MutableMap<String, Denom>.toProto(): BankProto.Bank.IbcDenomMap = BankProto.Bank.IbcDenomMap.newBuilder()
        .putAllIbcDenomToDenom(this
                .mapKeys{it.key}
                .mapValues{it.value.toString()})
        .build()
fun BankProto.Bank.IbcDenomMap.toCorda() = ibcDenomToDenomMap
        .mapValues{Denom.fromString(it.value)}
        .toMutableMap()

fun Bank.toProto(): BankProto.Bank = BankProto.Bank.newBuilder()
        .addAllParticipants(participants.map{Party(it.nameOrNull()!!, it.owningKey).toProto()})
        .setBaseId(baseId.toProto())
        .setAllocated(allocated.toProto())
        .setLocked(locked.toProto())
        .setMinted(minted.toProto())
        .setDenoms(denoms.toProto())
        .build()
fun BankProto.Bank.toCorda() = Bank(
        participants = participantsList.map{it.toCorda()},
        baseId = baseId.toCorda(),
        allocated = allocated.toCorda(),
        locked = locked.toCorda(),
        minted = minted.toCorda(),
        denoms = denoms.toCorda())

fun Map<Denom, Amount>.toProto(): CashBankProto.CashBank.SupplyMap = CashBankProto.CashBank.SupplyMap.newBuilder()
        .putAllDenomToAmount(this
                .mapKeys{it.key.toString()}
                .mapValues{it.value.toString()})
        .build()
fun CashBankProto.CashBank.SupplyMap.toCorda(): Map<Denom, Amount> = denomToAmountMap
        .mapKeys{Denom.fromString(it.key)}
        .mapValues{Amount.fromString(it.value)}

fun Map<String, Denom>.toProto(): CashBankProto.CashBank.IbcDenomMap = CashBankProto.CashBank.IbcDenomMap.newBuilder()
        .putAllIbcDenomToDenom(this
                .mapKeys{it.key}
                .mapValues{it.value.toString()})
        .build()
fun CashBankProto.CashBank.IbcDenomMap.toCorda(): Map<String, Denom> = ibcDenomToDenomMap
        .mapValues{Denom.fromString(it.value)}
        .toMap()

fun CashBank.toProto(): CashBankProto.CashBank = CashBankProto.CashBank.newBuilder()
        .addAllParticipants(participants.map{Party(it.nameOrNull()!!, it.owningKey).toProto()})
        .setBaseId(baseId.toProto())
        .setOwner(owner.toProto())
        .setSupply(supply.toProto())
        .setDenoms(denoms.toProto())
        .build()