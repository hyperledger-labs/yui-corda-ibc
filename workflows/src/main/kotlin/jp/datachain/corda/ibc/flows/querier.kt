package jp.datachain.corda.ibc.flows

import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.node.services.VaultService
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

fun VaultService.queryIbcHost(baseId: StateRef) : StateAndRef<Host>? = queryBy<Host>(
        QueryCriteria.LinearStateQueryCriteria(externalId = listOf(baseId.toString()))
).states.singleOrNull()

inline fun <reified T: IbcState> VaultService.queryIbcState(baseId: StateRef, id: Identifier) : StateAndRef<T>? = queryBy<T>(
        QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString()),
                uuid = listOf(id.toUUID())
        )
).states.singleOrNull()
