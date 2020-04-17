package jp.datachain.corda.ibc.contracts

import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.ConsensusState
import jp.datachain.corda.ibc.ics2.Evidence
import jp.datachain.corda.ibc.ics2.Header
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction

class Tao : Contract {
    override fun verify(tx: LedgerTransaction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    interface ClientCommands : CommandData {
        data class Initialise(val clientType: ClientType, val consensusState: ConsensusState) : TypeOnlyCommandData(), ClientCommands
        data class CheckValidityAndUpdateState(val header: Header)
        data class CheckMisbehaviourAndUpdateState(val evidence: Evidence)
    }

    interface ConnCommands : CommandData {
        class OpenInit : TypeOnlyCommandData(), ConnCommands
        class OpenTry : TypeOnlyCommandData(), ConnCommands
        class OpenAck : TypeOnlyCommandData(), ConnCommands
        class OpenConfirm : TypeOnlyCommandData(), ConnCommands
    }

    interface ChanCommands : CommandData {
        class OpenInit : TypeOnlyCommandData(), ChanCommands
        class OpenTry : TypeOnlyCommandData(), ChanCommands
        class OpenAck : TypeOnlyCommandData(), ChanCommands
        class OpenConfirm : TypeOnlyCommandData(), ChanCommands

        class CloseInit : TypeOnlyCommandData(), ChanCommands
        class CloseConfirm : TypeOnlyCommandData(), ChanCommands
    }
}