package jp.datachain.corda.ibc.ics24

import jp.datachain.corda.ibc.clients.corda.CordaCommitmentPrefix
import jp.datachain.corda.ibc.clients.corda.CordaConsensusState
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics2.ConsensusState
import jp.datachain.corda.ibc.states.IbcState
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Timestamp
import jp.datachain.corda.ibc.types.Version
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.nio.charset.Charset
import java.util.*

@BelongsToContract(Ibc::class)
data class Host private constructor (
        override val participants: List<AbstractParty>,
        override val baseId: StateRef,
        val notary: Party,
        val clientIds: List<Identifier>,
        val connIds: List<Identifier>,
        val portChanIds: List<Pair<Identifier, Identifier>>
) : IbcState {
    override val id = Identifier("host")

    constructor(seedAndRef: StateAndRef<HostSeed>) : this(
            seedAndRef.state.data.participants,
            seedAndRef.ref,
            seedAndRef.state.notary,
            emptyList(),
            emptyList(),
            emptyList()
    )

    fun getCurrentHeight() = Height(0)

    fun getStoredRecentConsensusStateCount() = 1

    fun getConsensusState(height: Height) : CordaConsensusState {
        require(height.height == 0L)
        return CordaConsensusState(Timestamp(0), Height(0), baseId, notary.owningKey)
    }

    fun getCommitmentPrefix() = CordaCommitmentPrefix()

    fun currentTimestamp() = Timestamp(0)

    fun getCompatibleVersions() = Version.Multiple(listOf(""))
    fun pickVersion(versions: Version.Multiple) = Version.Single(versions.versions.single())

    fun addClient(id: Identifier) : Host {
        require(!clientIds.contains(id))
        return copy(clientIds = clientIds + id)
    }

    fun addConnection(id: Identifier) : Host {
        require(!connIds.contains(id))
        return copy(connIds = connIds + id)
    }

    fun addPortChannel(portId: Identifier, chanId: Identifier) : Host {
        val portChanId = Pair(portId, chanId)
        require(!portChanIds.contains(portChanId))
        return copy(portChanIds = portChanIds + portChanId)
    }
}