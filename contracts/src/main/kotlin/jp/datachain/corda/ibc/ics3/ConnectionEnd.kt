package jp.datachain.corda.ibc.ics3

import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.types.Identifier
import jp.datachain.corda.ibc.types.Version

interface ConnectionEnd {
    val state: ConnectionState
    val counterpartyConnectionIdentifier: Identifier
    val counterpartyPrefix: CommitmentPrefix
    val clientIdentifier: Identifier
    val counterpartyClientIdentifier: Identifier
    val version: Version
}