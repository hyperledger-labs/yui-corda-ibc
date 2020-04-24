package jp.datachain.corda.ibc.states

import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics3.ConnectionEnd
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty

@BelongsToContract(Ibc::class)
data class Connection(
        override val participants: List<AbstractParty>,
        override val linearId: UniqueIdentifier,
        val connectionEnd: ConnectionEnd
) : IbcState
