package jp.datachain.corda.ibc.states

import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier

interface IbcState : LinearState {
    val id get() = Identifier(linearId)

    fun generateIdentifier() = Identifier(UniqueIdentifier(linearId.externalId))

    fun validateIdentifier(identifier: Identifier) = (identifier.toUniqueIdentifier().externalId == linearId.externalId)
}