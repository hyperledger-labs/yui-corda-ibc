package jp.datachain.corda.ibc.clients.corda

import jp.datachain.corda.ibc.ics2.ConsensusState
import jp.datachain.corda.ibc.ics2.Height
import jp.datachain.corda.ibc.types.Timestamp
import net.corda.core.contracts.StateRef
import java.security.PublicKey

data class CordaConsensusState(
        override val timestamp: Timestamp,
        override val height: Height,
        val baseId: StateRef,
        val notaryKey: PublicKey
) : ConsensusState
