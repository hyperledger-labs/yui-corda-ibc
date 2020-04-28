package jp.datachain.corda.ibc.contracts

import jp.datachain.corda.ibc.ics2.*
import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.HostSeed
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler.connOpenInit
import jp.datachain.corda.ibc.ics25.Handler.createClient
import jp.datachain.corda.ibc.states.Connection
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class Ibc : Contract {
    override fun verify(tx: LedgerTransaction) = tx.commandsOfType<Commands>().single().value.verify(tx)

    interface Commands : CommandData {
        fun verify(tx: LedgerTransaction)

        class HostSeedCreate : TypeOnlyCommandData(), Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "No state should be consumed" using (tx.inputs.size == 0)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                "Output type should be HostIdentifier" using (tx.outputs.single().data is HostSeed)
            }
        }

        class HostCreate : TypeOnlyCommandData(), Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val seed = tx.inRefsOfType<HostSeed>().single()
                val newHost = tx.outputsOfType<Host>().single()
                val expected = Host(seed, newHost.linearId.id)
                "Output should be expected state" using (newHost == expected)
            }
        }

        data class ClientCreate(val clientType: ClientType, val consensusState: ConsensusState) : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly two states should be created" using (tx.outputs.size == 2)
                val host = tx.inputsOfType<Host>().single()
                val newHost = tx.outputsOfType<Host>().single()
                val newClient = tx.outputsOfType<ClientState>().single()
                val expected = host.createClient(newClient.id, clientType, consensusState)
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

        data class ConnOpenInit(
                val desiredConnectionIdentifier: Identifier,
                val counterpartyPrefix: CommitmentPrefix,
                val clientIdentifier: Identifier,
                val counterpartyClientIdentifier: Identifier
        ) : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly two states should be consumed" using (tx.inputs.size == 2)
                "Exactly three states should be created" using (tx.outputs.size == 3)
                val host = tx.inputsOfType<Host>().single()
                val client = tx.inputsOfType<ClientState>().single()
                val newHost = tx.outputsOfType<Host>().single()
                val newClient = tx.outputsOfType<ClientState>().single()
                val newConn = tx.outputsOfType<Connection>().single()
                val expected = Pair(host, client).connOpenInit(newConn.id, desiredConnectionIdentifier, counterpartyPrefix, clientIdentifier, counterpartyClientIdentifier)
                "Outputs should be expected states" using (Triple(newHost, newClient, newClient) ==  expected)
            }
        }
        /*
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