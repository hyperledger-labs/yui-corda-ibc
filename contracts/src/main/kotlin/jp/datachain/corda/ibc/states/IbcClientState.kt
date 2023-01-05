package jp.datachain.corda.ibc.states

import com.google.protobuf.Any
import ibc.core.client.v1.Client
import ibc.lightclients.corda.v1.Corda
import ibc.lightclients.fabric.v1.Fabric
import ibc.lightclients.lcp.v1.Lcp
import ibc.lightclients.localhost.v1.Localhost
import ibc.lightclients.solomachine.v1.Solomachine
import ibc.lightclients.tendermint.v1.Tendermint
import jp.datachain.corda.ibc.clients.corda.CordaClientState
import jp.datachain.corda.ibc.clients.fabric.FabricClientState
import jp.datachain.corda.ibc.clients.lcp.LcpClientState
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
        val anyConsensusStates: Map<Client.Height, Any>
) : IbcState {
    constructor(host: Host, id: Identifier, anyClientState: Any, anyConsensusStates: Map<Client.Height, Any>)
            : this(host.participants, host.baseId, id, anyClientState, anyConsensusStates)

    val impl : ClientState = when {
        anyClientState.`is`(Corda.ClientState::class.java) -> CordaClientState(anyClientState, anyConsensusStates)
        anyClientState.`is`(Fabric.ClientState::class.java) -> FabricClientState(anyClientState, anyConsensusStates)
        anyClientState.`is`(Lcp.ClientState::class.java) -> LcpClientState(anyClientState, anyConsensusStates)
        anyClientState.`is`(Tendermint.ClientState::class.java) -> throw NotImplementedError()
        anyClientState.`is`(Solomachine.ClientState::class.java) -> throw NotImplementedError()
        anyClientState.`is`(Localhost.ClientState::class.java) -> throw NotImplementedError()
        else -> throw IllegalArgumentException()
    }

    fun update(newClientState: ClientState) = copy(
            anyClientState = newClientState.anyClientState,
            anyConsensusStates = newClientState.anyConsensusStates
    )
}
