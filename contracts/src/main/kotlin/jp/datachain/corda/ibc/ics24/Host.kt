package jp.datachain.corda.ibc.ics24

import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.states.IbcState
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import java.util.*

@BelongsToContract(Ibc::class)
data class Host private constructor (
        override val participants: List<AbstractParty>,
        override val linearId: UniqueIdentifier,
        val clientIds: List<Identifier>,
        val connIds: List<Identifier>,
        val portIds: List<Identifier>
) : IbcState {
    constructor(seedAndRef: StateAndRef<HostSeed>, uuid: UUID) : this(
            seedAndRef.state.data.participants,
            UniqueIdentifier(externalId = seedAndRef.ref.toString(), id = uuid),
            emptyList(),
            emptyList(),
            emptyList()
    )

    fun addClient(id: Identifier) : Host {
        require(validateIdentifier(id))
        require(!clientIds.contains(id))
        return copy(clientIds = clientIds + id)
    }

    fun addConnection(id: Identifier) : Host {
        require(validateIdentifier(id))
        require(!connIds.contains(id))
        return copy(connIds = connIds + id)
    }

    fun addPort(id: Identifier) : Host {
        require(validateIdentifier(id))
        require(!portIds.contains(id))
        return copy(portIds = portIds + id)
    }
}
