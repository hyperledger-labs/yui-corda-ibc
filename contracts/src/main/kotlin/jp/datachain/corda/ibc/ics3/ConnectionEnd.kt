package jp.datachain.corda.ibc.ics3

import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.types.Version
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class ConnectionEnd(
        val state: ConnectionState,
        val counterpartyConnectionIdentifier: Identifier,
        val counterpartyPrefix: CommitmentPrefix,
        val clientIdentifier: Identifier,
        val counterpartyClientIdentifier: Identifier,
        val versions: List<Version>
) {
    constructor(
            state: ConnectionState,
            counterpartyConnectionIdentifier: Identifier,
            counterpartyPrefix: CommitmentPrefix,
            clientIdentifier: Identifier,
            counterpartyClientIdentifier: Identifier,
            version: Version
    ) : this(
            state,
            counterpartyConnectionIdentifier,
            counterpartyPrefix,
            clientIdentifier,
            counterpartyClientIdentifier,
            listOf(version)
    )

    val version
        get() = versions.single()
}