package jp.datachain.corda.ibc.contracts

import jp.datachain.corda.ibc.ics2.*
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.HostSeed
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler.createClient
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import java.util.*

class Ibc : Contract {
    override fun verify(tx: LedgerTransaction) = tx.commandsOfType<Commands>().single().value.verify(tx)

    interface Commands : CommandData {
        fun verify(tx: LedgerTransaction)

        class HostSeedCreate : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "No state should be consumed" using (tx.inputs.size == 0)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                "Output type should be HostIdentifier" using (tx.outputs.single().data is HostSeed)
            }
        }

        data class HostCreate(val uuid: UUID) : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val seed= tx.inRefsOfType<HostSeed>().single()
                val newHost = tx.outputsOfType<Host>().single()
                val expected = Host(seed, uuid)
                "Output should be expected state" using (newHost == expected)
            }
        }

        data class ClientCreate(val id: Identifier, val clientType: ClientType, val consensusState: ConsensusState) : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly two states should be created" using (tx.outputs.size == 2)
                val host = tx.inputsOfType<Host>().single()
                val newHost = tx.outputsOfType<Host>().single()
                val newClient = tx.outputsOfType<ClientState>().single()
                val expected = host.createClient(id, clientType, consensusState)
                "Outputs should be expected states" using (Pair(newHost, newClient) == expected)
            }
        }

        data class ClientUpdate(val header: Header) : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val client = tx.inputsOfType<ClientState>().single()
                val newClient = tx.outputsOfType<ClientState>().single()
                val expected = client.checkValidityAndUpdateState(header)
                "Output should be expected state" using (newClient ==  expected)
            }
        }

        data class ClientMisbehaviour(val evidence: Evidence) : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val client = tx.inputsOfType<ClientState>().single()
                val newClient = tx.outputsOfType<ClientState>().single()
                val expected = client.checkMisbehaviourAndUpdateState(evidence)
                "Output should be expected state" using (newClient ==  expected)
            }
        }

        /*
        data class ConnOpenInit : TypeOnlyCommandData(), Commands
        class ConnOpenTry : TypeOnlyCommandData(), Commands
        class ConnOpenAck : TypeOnlyCommandData(), Commands
        class ConnOpenConfirm : TypeOnlyCommandData(), Commands
         */

        /*
        class ChanOpenInit : TypeOnlyCommandData(), Commands
        class ChanOpenTry : TypeOnlyCommandData(), Commands
        class ChanOpenAck : TypeOnlyCommandData(), Commands
        class ChanOpenConfirm : TypeOnlyCommandData(), Commands
        class ChanCloseInit : TypeOnlyCommandData(), Commands
        class ChanCloseConfirm : TypeOnlyCommandData(), Commands
        */
    }
}