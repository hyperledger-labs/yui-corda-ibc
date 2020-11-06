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
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics3.ConnectionEnd
import jp.datachain.corda.ibc.ics3.ConnectionState
import jp.datachain.corda.ibc.ics4.*
import jp.datachain.corda.ibc.states.Channel
import jp.datachain.corda.ibc.states.Connection
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Quadruple
import jp.datachain.corda.ibc.types.Version

object Handler {
    fun createClient(
            ctx: Context,
            id: Identifier,
            clientType: ClientType,
            consensusState: ConsensusState
    ) {
        val host = ctx.getInput<Host>()
        when (clientType) {
            ClientType.CordaClient -> {
                val host = host.addClient(id)
                val consensusState = consensusState as CordaConsensusState
                val client = CordaClientState(host, id, consensusState)
                ctx.addOutput(host)
                ctx.addOutput(client)
            }
            else -> throw NotImplementedError()
        }
    }

    fun connOpenInit(
            ctx: Context,
            identifier: Identifier,
            desiredCounterpartyConnectionIdentifier: Identifier,
            counterpartyPrefix: CommitmentPrefix,
            clientIdentifier: Identifier,
            counterpartyClientIdentifier: Identifier,
            version: Version?
    ) {
        val host = ctx.getInput<Host>().addConnection(identifier)
        val client = ctx.getInput<ClientState>().addConnection(identifier)

        require(host.clientIds.contains(clientIdentifier)){"unknown client"}
        require(client.id == clientIdentifier){"mismatch client"}

        val versions = if (version != null) {
            require(host.getCompatibleVersions().contains(version)){"incompatible version"}
            listOf(version)
        } else {
            host.getCompatibleVersions()
        }
        val end = ConnectionEnd(
                ConnectionState.INIT,
                desiredCounterpartyConnectionIdentifier,
                counterpartyPrefix,
                clientIdentifier,
                counterpartyClientIdentifier,
                versions
        )

        ctx.addOutput(host)
        ctx.addOutput(client)
        ctx.addOutput(Connection(host, identifier, end))
    }

    fun connOpenTry(
            ctx: Context,
            desiredIdentifier: Identifier,
            counterpartyChosenConnectionIdentifier: Identifier,
            counterpartyConnectionIdentifier: Identifier,
            counterpartyPrefix: CommitmentPrefix,
            counterpartyClientIdentifier: Identifier,
            clientIdentifier: Identifier,
            counterpartyVersions: List<Version>,
            proofInit: CommitmentProof,
            proofConsensus: CommitmentProof,
            proofHeight: Height,
            consensusHeight: Height
    ) {
        val host = ctx.getInput<Host>()
        val client = ctx.getInput<ClientState>()
        val previous = ctx.getInputOrNull<Connection>()

        if (previous != null) {
            require(host.connIds.contains(desiredIdentifier)){"unknown connection in host"}
            require(client.connIds.contains(desiredIdentifier)){"unknown connection in client"}
            require(previous.id == desiredIdentifier){"mismatch connection"}
        }
        require(host.clientIds.contains(client.id)){"unknown client"}
        require(clientIdentifier == client.id){"mismatch client"}

        require(counterpartyChosenConnectionIdentifier == Identifier("") ||
                counterpartyChosenConnectionIdentifier == desiredIdentifier)

        require(previous == null ||
                (previous.end.state == ConnectionState.INIT &&
                        previous.end.counterpartyConnectionIdentifier == counterpartyConnectionIdentifier &&
                        previous.end.counterpartyPrefix == counterpartyPrefix &&
                        previous.end.clientIdentifier == clientIdentifier &&
                        previous.end.counterpartyClientIdentifier == counterpartyClientIdentifier)
        ){"invalid previous state"}

        val versionsIntersection = counterpartyVersions.intersect(if (previous != null) { previous.end.versions } else { host.getCompatibleVersions() })
        val version = host.pickVersion(versionsIntersection)

        val expected = ConnectionEnd(
                ConnectionState.INIT,
                counterpartyChosenConnectionIdentifier,
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

        val identifier = desiredIdentifier
        val connectionEnd = ConnectionEnd(
                ConnectionState.TRYOPEN,
                counterpartyConnectionIdentifier,
                counterpartyPrefix,
                clientIdentifier,
                counterpartyClientIdentifier,
                version)
        ctx.addOutput(Connection(host, identifier, connectionEnd))
        ctx.addOutput(host.addConnection(identifier))
        ctx.addOutput(client.addConnection(identifier))
    }

    fun connOpenAck(
            ctx: Context,
            identifier: Identifier,
            version: Version,
            counterpartyIdentifier: Identifier,
            proofTry: CommitmentProof,
            proofConsensus: CommitmentProof,
            proofHeight: Height,
            consensusHeight: Height
    ) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getInput<Connection>()

        require(host.clientIds.contains(client.id)){"unknown client"}
        require(host.connIds.contains(conn.id)){"unknown connection in host"}
        require(client.connIds.contains(conn.id)){"unknown connection in client"}
        require(conn.id == identifier){"mismatch connection"}
        require(client.id == conn.end.clientIdentifier){"mismatch client"}

        require(consensusHeight <= host.getCurrentHeight()){"unknown height"}
        require(conn.end.counterpartyConnectionIdentifier == Identifier("") ||
                counterpartyIdentifier == conn.end.counterpartyConnectionIdentifier)
        require(conn.end.state == ConnectionState.INIT && conn.end.versions.contains(version) ||
                conn.end.state == ConnectionState.TRYOPEN || conn.end.version == version){"invalid connection state"}

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
                counterpartyIdentifier,
                expected)){"connection verification failure"}

        val expectedConsensusState = host.getConsensusState(consensusHeight)
        require(client.verifyClientConsensusState(
                proofHeight,
                conn.end.counterpartyPrefix,
                proofConsensus,
                conn.end.counterpartyClientIdentifier,
                consensusHeight,
                expectedConsensusState)){"client consensus verification failure"}

        ctx.addOutput(conn.copy(end = conn.end.copy(state = ConnectionState.OPEN,  versions = listOf(version))))
    }

    fun connOpenConfirm(
            ctx: Context,
            identifier: Identifier,
            proofAck: CommitmentProof,
            proofHeight: Height
    ) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getInput<Connection>()

        require(host.clientIds.contains(client.id)){"unknown client"}
        require(host.connIds.contains(conn.id)){"unknown connection in host"}
        require(client.connIds.contains(conn.id)){"unknown connection in client"}
        require(conn.id == identifier){"mismatch connection"}
        require(client.id == conn.end.clientIdentifier){"mismatch client"}

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

        ctx.addOutput(conn.copy(end = conn.end.copy(state = ConnectionState.OPEN)))
    }

    fun chanOpenInit(
            ctx: Context,
            order: ChannelOrder,
            connectionHops: List<Identifier>,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: Version
    ) {
        // TODO: port authentication should be added somehow

        val host = ctx.getInput<Host>()
        val conn = ctx.getReference<Connection>()

        require(host.connIds.contains(conn.id))

        require(conn.id == connectionHops.single())

        val end = ChannelEnd(
                ChannelState.INIT,
                order,
                counterpartyPortIdentifier,
                counterpartyChannelIdentifier,
                connectionHops,
                version)

        ctx.addOutput(host.addPortChannel(portIdentifier, channelIdentifier))
        ctx.addOutput(Channel(host, portIdentifier, channelIdentifier, end))
    }

    fun chanOpenTry(
            ctx: Context,
            order: ChannelOrder,
            connectionHops: List<Identifier>,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyChosenChannelIdentifer: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: Version,
            counterpartyVersion: Version,
            proofInit: CommitmentProof,
            proofHeight: Height
    ) {
        val host = ctx.getInput<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getReference<Connection>()
        val previous = ctx.getInputOrNull<Channel>()

        if (previous != null) {
            require(host.portChanIds.contains(Pair(portIdentifier, channelIdentifier)))
            require(previous.portId == portIdentifier)
            require(previous.id == channelIdentifier)
        }
        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(client.connIds.contains(conn.id))
        require(conn.id == connectionHops.single())
        require(client.id == conn.end.clientIdentifier)

        require(counterpartyChosenChannelIdentifer == Identifier("") ||
                counterpartyChosenChannelIdentifer == channelIdentifier)

        require(previous == null ||
                ( previous.end.state == ChannelState.INIT &&
                        previous.end.ordering == order &&
                        previous.end.counterpartyPortIdentifier == counterpartyPortIdentifier &&
                        previous.end.counterpartyChannelIdentifier == counterpartyChannelIdentifier &&
                        previous.end.connectionHops == connectionHops &&
                        previous.end.version == version))

        require(conn.end.state == ConnectionState.OPEN)

        val expected = ChannelEnd(
                ChannelState.INIT,
                order,
                portIdentifier,
                counterpartyChosenChannelIdentifer,
                listOf(conn.end.counterpartyConnectionIdentifier),
                counterpartyVersion)
        require(client.verifyChannelState(
                proofHeight,
                conn.end.counterpartyPrefix,
                proofInit,
                counterpartyPortIdentifier,
                counterpartyChannelIdentifier,
                expected))

        val end = ChannelEnd(
                ChannelState.TRYOPEN,
                order,
                counterpartyPortIdentifier,
                counterpartyChannelIdentifier,
                connectionHops,
                version)

        val chan = Channel(host, portIdentifier, channelIdentifier, end)
        ctx.addOutput(host.addPortChannel(portIdentifier, channelIdentifier))
        if (previous == null) {
            ctx.addOutput(chan)
        } else {
            ctx.addOutput(chan.copy(
                    nextSequenceAck = previous.nextSequenceAck,
                    nextSequenceSend = previous.nextSequenceSend,
                    nextSequenceRecv = previous.nextSequenceRecv))
        }
    }

    fun chanOpenAck(
            ctx: Context,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyVersion: Version,
            counterpartyChannelIdentifier: Identifier,
            proofTry: CommitmentProof,
            proofHeight: Height
    ) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getReference<Connection>()
        val chan = ctx.getInput<Channel>()

        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(client.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))
        require(chan.portId == portIdentifier)
        require(chan.id == channelIdentifier)
        require(client.id == conn.end.clientIdentifier)

        require(chan.end.state == ChannelState.INIT || chan.end.state == ChannelState.TRYOPEN)

        require(chan.end.counterpartyChannelIdentifier == Identifier("") ||
                counterpartyChannelIdentifier == chan.end.counterpartyChannelIdentifier)

        require(conn.id == chan.end.connectionHops.single())
        require(conn.end.state == ConnectionState.OPEN)

        val expected = ChannelEnd(
                ChannelState.TRYOPEN,
                chan.end.ordering,
                portIdentifier,
                channelIdentifier,
                listOf(conn.end.counterpartyConnectionIdentifier),
                counterpartyVersion)
        require(client.verifyChannelState(
                proofHeight,
                conn.end.counterpartyPrefix,
                proofTry,
                chan.end.counterpartyPortIdentifier,
                chan.end.counterpartyChannelIdentifier,
                expected))

        ctx.addOutput(chan.copy(end = chan.end.copy(
                state = ChannelState.OPEN,
                version = counterpartyVersion,
                counterpartyChannelIdentifier = counterpartyChannelIdentifier
        )))
    }

    fun chanOpenConfirm(
            ctx: Context,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            proofAck: CommitmentProof,
            proofHeight: Height
    ) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getReference<Connection>()
        val chan = ctx.getInput<Channel>()

        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(client.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))
        require(chan.portId == portIdentifier)
        require(chan.id == channelIdentifier)
        require(client.id == conn.end.clientIdentifier)

        require(chan.end.state == ChannelState.TRYOPEN)

        require(conn.id == chan.end.connectionHops.single())
        require(conn.end.state == ConnectionState.OPEN)

        val expected = ChannelEnd(
                ChannelState.OPEN,
                chan.end.ordering,
                portIdentifier,
                channelIdentifier,
                listOf(conn.end.counterpartyConnectionIdentifier),
                chan.end.version)
        require(client.verifyChannelState(
                proofHeight,
                conn.end.counterpartyPrefix,
                proofAck,
                chan.end.counterpartyPortIdentifier,
                chan.end.counterpartyChannelIdentifier,
                expected))

        ctx.addOutput(chan.copy(end = chan.end.copy(state = ChannelState.OPEN)))
    }

    fun chanCloseInit(
            ctx: Context,
            portIdentifier: Identifier,
            channelIdentifier: Identifier
    ) {
        val host = ctx.getReference<Host>()
        val conn = ctx.getReference<Connection>()
        val chan = ctx.getInput<Channel>()

        require(host.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))
        require(chan.portId == portIdentifier)
        require(chan.id == channelIdentifier)

        require(chan.end.state != ChannelState.CLOSED)

        require(conn.id == chan.end.connectionHops.single())
        require(conn.end.state == ConnectionState.OPEN)

        ctx.addOutput(chan.copy(end = chan.end.copy(state = ChannelState.CLOSED)))
    }

    fun Quadruple<Host, ClientState, Connection, Channel>.chanCloseConfirm(
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            proofInit: CommitmentProof,
            proofHeight: Height
    ) : Channel {
        val host = this.first
        val client = this.second
        val conn = this.third
        val chan = this.fourth

        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(client.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))
        require(chan.portId == portIdentifier)
        require(chan.id == channelIdentifier)

        require(chan.end.state != ChannelState.CLOSED)

        require(conn.id == chan.end.connectionHops.single())

        require(conn.end.state == ConnectionState.OPEN)

        val expected = ChannelEnd(
                ChannelState.CLOSED,
                chan.end.ordering,
                portIdentifier,
                channelIdentifier,
                listOf(conn.end.counterpartyConnectionIdentifier),
                chan.end.version)
        require(client.verifyChannelState(
                proofHeight,
                conn.end.counterpartyPrefix,
                proofInit,
                chan.end.counterpartyPortIdentifier,
                chan.end.counterpartyChannelIdentifier,
                expected))

        return chan.copy(end = chan.end.copy(state = ChannelState.CLOSED))
    }

    fun sendPacket(ctx: Context, packet: Packet) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getReference<Connection>()
        val chan = ctx.getInput<Channel>()

        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))
        require(client.connIds.contains(conn.id))

        require(chan.end.state != ChannelState.CLOSED)

        require(packet.sourcePort == chan.portId)
        require(packet.sourceChannel == chan.id)
        require(packet.destPort == chan.end.counterpartyPortIdentifier)
        require(packet.destChannel == chan.end.counterpartyChannelIdentifier)

        require(conn.id == chan.end.connectionHops.single())

        require(conn.end.clientIdentifier == client.id)
        val latestClientHeight = client.latestClientHeight()
        require(packet.timeoutHeight.height == 0L || latestClientHeight.height < packet.timeoutHeight.height)

        require(packet.sequence == chan.nextSequenceSend)

        ctx.addOutput(chan.copy(
                nextSequenceSend = chan.nextSequenceSend + 1,
                packets = chan.packets + mapOf(packet.sequence to packet)))
    }

    fun Quadruple<Host, ClientState, Connection, Channel>.recvPacket(
            packet: Packet,
            proof: CommitmentProof,
            proofHeight: Height,
            acknowledgement: Acknowledgement
    ) : Channel {
        val host = this.first
        val client = this.second
        val conn = this.third
        var chan = this.fourth

        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))
        require(client.connIds.contains(conn.id))

        require(packet.destPort == chan.portId)
        require(packet.destChannel == chan.id)

        require(chan.end.state == ChannelState.OPEN)
        require(packet.sourcePort == chan.end.counterpartyPortIdentifier)
        require(packet.sourceChannel == chan.end.counterpartyChannelIdentifier)

        require(conn.id == chan.end.connectionHops.single())
        require(conn.end.state == ConnectionState.OPEN)

        require(packet.timeoutHeight.height == 0L || host.getCurrentHeight().height < packet.timeoutHeight.height)
        require(packet.timeoutTimestamp.timestamp == 0 || host.currentTimestamp().timestamp < packet.timeoutTimestamp.timestamp)

        require(client.verifyPacketData(
                proofHeight,
                conn.end.counterpartyPrefix,
                proof,
                packet.sourcePort,
                packet.sourceChannel,
                packet.sequence,
                packet))

        if (acknowledgement.data.size > 0 || chan.end.ordering == ChannelOrder.UNORDERED) {
            chan = chan.copy(acknowledgements = chan.acknowledgements + mapOf(packet.sequence to acknowledgement))
        }

        if (chan.end.ordering == ChannelOrder.ORDERED) {
            require(packet.sequence == chan.nextSequenceRecv)
            chan = chan.copy(nextSequenceRecv = chan.nextSequenceRecv + 1)
        }

        return chan
    }

    fun Quadruple<Host, ClientState, Connection, Channel>.acknowledgePacket(
            packet: Packet,
            acknowledgement: Acknowledgement,
            proof: CommitmentProof,
            proofHeight: Height
    ) : Channel {
        val host = this.first
        val client = this.second
        val conn = this.third
        var chan = this.fourth

        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))
        require(client.connIds.contains(conn.id))

        require(packet.sourcePort == chan.portId)
        require(packet.sourceChannel == chan.id)

        require(chan.end.state == ChannelState.OPEN)

        require(packet.destPort == chan.end.counterpartyPortIdentifier)
        require(packet.destChannel == chan.end.counterpartyChannelIdentifier)

        require(conn.id == chan.end.connectionHops.single())
        require(conn.end.state == ConnectionState.OPEN)

        require(chan.packets[packet.sequence] == packet)

        require(client.verifyPacketAcknowledgement(
                proofHeight,
                conn.end.counterpartyPrefix,
                proof,
                packet.destPort,
                packet.destChannel,
                packet.sequence,
                acknowledgement))

        if (chan.end.ordering == ChannelOrder.ORDERED) {
            require(packet.sequence == chan.nextSequenceAck)
            chan = chan.copy(nextSequenceAck = chan.nextSequenceAck + 1)
        }

        chan = chan.copy(packets = chan.packets - packet.sequence)
        return chan
    }
}
