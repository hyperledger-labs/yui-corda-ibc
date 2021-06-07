package jp.datachain.corda.ibc.ics20cash

import ibc.applications.transfer.v1.Transfer
import jp.datachain.corda.ibc.contracts.Ibc
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import java.security.PublicKey

@BelongsToContract(Ibc::class)
@CordaSerializable
data class Voucher(
        override val participants: List<AbstractParty>,
        override val owner: AbstractParty,
        override val amount: Amount<Issued<Transfer.DenomTrace>>
): FungibleAsset<Transfer.DenomTrace> {
    override val exitKeys: Collection<PublicKey> get() = setOf(owner.owningKey, amount.token.issuer.party.owningKey)

    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        throw NotImplementedError()
    }
    override fun withNewOwnerAndAmount(newAmount: Amount<Issued<Transfer.DenomTrace>>, newOwner: AbstractParty): FungibleAsset<Transfer.DenomTrace> {
        throw NotImplementedError()
    }
}