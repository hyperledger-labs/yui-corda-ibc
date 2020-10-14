package jp.datachain.corda.ibc.states

import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics3.ConnectionEnd
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.StateRef
import net.corda.core.identity.AbstractParty

@BelongsToContract(Ibc::class)
data class Connection private constructor (
        override val participants: List<AbstractParty>,
        override val baseId: StateRef,
        override val id: Identifier,
        val end: ConnectionEnd
) : IbcState {
    constructor(host: Host, id: Identifier, connectionEnd: ConnectionEnd)
            : this(host.participants, host.baseId, id, connectionEnd)
}
