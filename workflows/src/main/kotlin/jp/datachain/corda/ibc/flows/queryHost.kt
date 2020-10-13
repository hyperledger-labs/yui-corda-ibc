package jp.datachain.corda.ibc.flows

import jp.datachain.corda.ibc.ics24.Host
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.services.VaultService
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

fun VaultService.queryHost(externalId: String) : StateAndRef<Host> = queryBy<Host>(
        QueryCriteria.LinearStateQueryCriteria(externalId = listOf(externalId))
).states.single()