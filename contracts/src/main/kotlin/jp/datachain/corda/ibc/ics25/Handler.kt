package jp.datachain.corda.ibc.ics25

import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.ConsensusState
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.clients.corda.CordaClientState
import jp.datachain.corda.ibc.clients.corda.CordaConsensusState
import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics3.ConnectionEnd
import jp.datachain.corda.ibc.ics3.ConnectionState
import jp.datachain.corda.ibc.states.Connection
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Version

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

    fun Pair<Host, ClientState>.connOpenInit(
            identifier: Identifier,
            desiredCounterpartyConnectionIdentifier: Identifier,
            counterpartyPrefix: CommitmentPrefix,
            clientIdentifier: Identifier,
            counterpartyClientIdentifier: Identifier
    ) : Triple<Host, ClientState, Connection> {
        val host = this.first.addConnection(identifier)
        val client = this.second.addConnection(identifier)

        require(host.clientIds.contains(client.id)){"unknown client"}
        require(clientIdentifier == client.id){"mismatch client"}

        val end = ConnectionEnd(
                ConnectionState.INIT,
                desiredCounterpartyConnectionIdentifier,
                counterpartyPrefix,
                clientIdentifier,
                counterpartyClientIdentifier,
                host.getCompatibleVersions()
        )

        return Triple(host, client, Connection(host, identifier, end))
    }

    fun Triple<Host, ClientState, Connection?>.connOpenTry(
            desiredIdentifier: Identifier,
            counterpartyConnectionIdentifier: Identifier,
            counterpartyPrefix: CommitmentPrefix,
            counterpartyClientIdentifier: Identifier,
            clientIdentifier: Identifier,
            counterpartyVersions: Version.Multiple,
            proofInit: CommitmentProof,
            proofConsensus: CommitmentProof,
            proofHeight: Height,
            consensusHeight: Height
    ) : Triple<Host, ClientState, Connection> {
        val previous = this.third
        val (host, client) = if (previous == null) {
            Pair(this.first.addConnection(desiredIdentifier), this.second.addConnection(desiredIdentifier))
        } else {
            require(this.second.connIds.contains(desiredIdentifier)){"unknown connection"}
            require(previous.id == desiredIdentifier){"mismatch connection"}
            Pair(this.first, this.second)
        }

        require(host.clientIds.contains(client.id)){"unknown client"}
        require(clientIdentifier == client.id){"mismatch client"}

        val expected = ConnectionEnd(
                ConnectionState.INIT,
                desiredIdentifier,
                host.getCommitmentPrefix(),
                counterpartyClientIdentifier,
                clientIdentifier,
                counterpartyVersions)
        require(client.verifyConnectionState(
                proofHeight,
                counterpartyPrefix,
                proofInit,
                counterpartyConnectionIdentifier,
                expected)){"connection verification failure"}

        val expectedConsensusState = host.getConsensusState(consensusHeight)
        require(client.verifyClientConsensusState(
                proofHeight,
                counterpartyPrefix,
                proofConsensus,
                counterpartyClientIdentifier,
                consensusHeight,
                expectedConsensusState)){"client consensus verification failure"}

        val version = host.pickVersion(counterpartyVersions)
        val connectionEnd = ConnectionEnd(
                ConnectionState.TRYOPEN,
                counterpartyConnectionIdentifier,
                counterpartyPrefix,
                clientIdentifier,
                counterpartyClientIdentifier,
                version)
        require(previous == null ||
                (previous.end.state == ConnectionState.INIT &&
                        previous.end.counterpartyConnectionIdentifier == counterpartyConnectionIdentifier &&
                        previous.end.counterpartyPrefix == counterpartyPrefix &&
                        previous.end.clientIdentifier == clientIdentifier &&
                        previous.end.counterpartyClientIdentifier == counterpartyClientIdentifier &&
                        previous.end.version == version)){"invalid previous state"}

        return Triple(host, client, Connection(host, desiredIdentifier, connectionEnd))
    }

    fun Triple<Host, ClientState, Connection>.connOpenAck(
            identifier: Identifier,
            version: Version.Single,
            proofTry: CommitmentProof,
            proofConsensus: CommitmentProof,
            proofHeight: Height,
            consensusHeight: Height
    ) : Connection {
        val host = this.first
        val client = this.second
        val conn = this.third

        require(host.clientIds.contains(client.id)){"unknown client"}
        require(host.connIds.contains(conn.id)){"unknown connection in host"}
        require(client.connIds.contains(conn.id)){"unknown connection in client"}
        require(conn.id == identifier){"mismatch connection"}

        require(consensusHeight.height <= host.getCurrentHeight().height){"unknown height"}
        require(conn.end.state == ConnectionState.INIT || conn.end.state == ConnectionState.TRYOPEN){"invalid connection state"}

        val expected = ConnectionEnd(
                ConnectionState.TRYOPEN,
                identifier,
                host.getCommitmentPrefix(),
                conn.end.counterpartyClientIdentifier,
                conn.end.clientIdentifier,
                version)
        require(client.verifyConnectionState(
                proofHeight,
                conn.end.counterpartyPrefix,
                proofTry,
                conn.end.counterpartyConnectionIdentifier,
                expected)){"connection verification failure"}

        val expectedConsensusState = host.getConsensusState(consensusHeight)
        require(client.verifyClientConsensusState(
                proofHeight,
                conn.end.counterpartyPrefix,
                proofConsensus,
                conn.end.counterpartyClientIdentifier,
                consensusHeight,
                expectedConsensusState)){"client consensus verification failure"}

        require(host.getCompatibleVersions().versions.contains(version.version)){"incompatible version"}

        return conn.copy(end = conn.end.copy(state = ConnectionState.OPEN,  version = version))
    }

    fun Triple<Host, ClientState, Connection>.connOpenConfirm(
            identifier: Identifier,
            proofAck: CommitmentProof,
            proofHeight: Height
    ) : Connection {
        val host = this.first
        val client = this.second
        val conn = this.third

        require(host.clientIds.contains(client.id)){"unknown client"}
        require(host.connIds.contains(conn.id)){"unknown connection in host"}
        require(client.connIds.contains(conn.id)){"unknown connection in client"}
        require(conn.id == identifier){"mismatch connection"}

        require(conn.end.state == ConnectionState.TRYOPEN){"invalid connection state"}
        val expected = ConnectionEnd(
                ConnectionState.OPEN,
                identifier,
                host.getCommitmentPrefix(),
                conn.end.counterpartyClientIdentifier,
                conn.end.clientIdentifier,
                conn.end.version)
        require(client.verifyConnectionState(
                proofHeight,
                conn.end.counterpartyPrefix,
                proofAck,
                conn.end.counterpartyConnectionIdentifier,
                expected)){"connection verification failure"}

        return conn.copy(end = conn.end.copy(state = ConnectionState.OPEN))
    }
}
