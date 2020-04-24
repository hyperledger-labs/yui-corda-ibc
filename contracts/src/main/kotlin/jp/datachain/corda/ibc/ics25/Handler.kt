package jp.datachain.corda.ibc.ics25

import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.ConsensusState
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.clients.corda.CordaClientState
import jp.datachain.corda.ibc.clients.corda.CordaConsensusState
import jp.datachain.corda.ibc.ics24.Identifier

object Handler {
    fun Host.createClient(
            id: Identifier,
            clientType: ClientType,
            consensusState: ConsensusState
    ): Pair<Host, ClientState> {
        when (clientType) {
            ClientType.CordaClient -> {
                val host = addClient(id)
                val consensusState = consensusState as CordaConsensusState
                val client = CordaClientState(host, id, consensusState)
                return Pair(host, client)
            }
            else -> throw NotImplementedError()
        }
    }
}
