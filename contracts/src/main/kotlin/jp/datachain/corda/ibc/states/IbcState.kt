package jp.datachain.corda.ibc.states

import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier

interface IbcState : LinearState {
    val baseId: StateRef
    val id: Identifier

    override val linearId: UniqueIdentifier
        get() = UniqueIdentifier(baseId.toString(), id.toUUID())
}