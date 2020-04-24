package jp.datachain.corda.ibc.clients.corda

import jp.datachain.corda.ibc.ics2.ConsensusState
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Timestamp
import java.security.PublicKey

data class CordaConsensusState(
        override val timestamp: Timestamp,
        val height: Height,
        val notaryKey: PublicKey
) : ConsensusState
