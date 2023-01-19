package jp.datachain.corda.ibc.states

import com.google.protobuf.Any
import ibc.core.client.v1.Client
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.StateRef
import net.corda.core.identity.AbstractParty

@BelongsToContract(Ibc::class)
data class IbcClientState private constructor (
        override val participants: List<AbstractParty>,
        override val baseId: StateRef,
        override val id: Identifier,
        val anyClientState: Any,
        val anyConsensusStates: Map<Client.Height, Any>,
        val impl: ClientState
) : IbcState {
    constructor(host: Host, id: Identifier, clientState: ClientState)
            : this(host.participants, host.baseId, id, clientState.anyClientState, clientState.anyConsensusStates, clientState)

    fun update(newClientState: ClientState) = copy(
            anyClientState = newClientState.anyClientState,
            anyConsensusStates = newClientState.anyConsensusStates,
            impl = newClientState
    )
}