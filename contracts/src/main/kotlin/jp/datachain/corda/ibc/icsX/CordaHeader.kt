package jp.datachain.corda.ibc.icsX

import jp.datachain.corda.ibc.ics2.Header
import net.corda.core.transactions.NotaryChangeLedgerTransaction

// Consensus state update in the context of Corda is notary change transaction
data class CordaHeader(val notaryChangeTx: NotaryChangeLedgerTransaction) : Header
