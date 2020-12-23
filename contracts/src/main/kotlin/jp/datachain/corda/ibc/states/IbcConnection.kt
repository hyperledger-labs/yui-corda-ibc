package jp.datachain.corda.ibc.states

import ibc.core.connection.v1.Connection
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.StateRef
import net.corda.core.identity.AbstractParty

@BelongsToContract(Ibc::class)
data class IbcConnection private constructor (
        override val participants: List<AbstractParty>,
        override val baseId: StateRef,
        override val id: Identifier,
        val end: Connection.ConnectionEnd
) : IbcState {
    constructor(host: Host, id: Identifier, connectionEnd: Connection.ConnectionEnd)
            : this(host.participants, host.baseId, id, connectionEnd)
}
