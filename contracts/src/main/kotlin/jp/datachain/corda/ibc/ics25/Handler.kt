package jp.datachain.corda.ibc.ics25

import ibc.core.client.v1.Client.Height
import ibc.core.client.v1.compareTo
import ibc.core.client.v1.isZero
import ibc.core.connection.v1.Connection
import ibc.core.connection.v1.Tx
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.ConsensusState
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.clients.corda.CordaClientState
import jp.datachain.corda.ibc.clients.corda.CordaConsensusState
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics4.*
import jp.datachain.corda.ibc.states.IbcChannel
import jp.datachain.corda.ibc.states.IbcConnection

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

    fun connOpenInit(ctx: Context, msg: Tx.MsgConnectionOpenInit) {
        val host = ctx.getInput<Host>().addConnection(Identifier(msg.connectionId))
        val client = ctx.getInput<ClientState>().addConnection(Identifier(msg.connectionId))

        require(host.clientIds.contains(Identifier(msg.clientId))){"unknown client"}
        require(client.id == Identifier(msg.clientId)){"mismatch client"}

        val versions = if (msg.hasVersion()) {
            require(host.getCompatibleVersions().contains(msg.version)){"incompatible version"}
            listOf(msg.version)
        } else {
            host.getCompatibleVersions()
        }
        val end = Connection.ConnectionEnd.newBuilder()
                .setClientId(msg.clientId)
                .addAllVersions(versions)
                .setState(Connection.State.STATE_INIT)
                .setCounterparty(msg.counterparty)
                .build()

        ctx.addOutput(host)
        ctx.addOutput(client)
        ctx.addOutput(IbcConnection(host, Identifier(msg.connectionId), end))
    }

    fun connOpenTry(ctx: Context, msg: Tx.MsgConnectionOpenTry) {
        val host = ctx.getInput<Host>()
        val client = ctx.getInput<ClientState>()
        val previous = ctx.getInputOrNull<IbcConnection>()

        if (previous != null) {
            require(host.connIds.contains(Identifier(msg.desiredConnectionId))){"unknown connection in host"}
            require(client.connIds.contains(Identifier(msg.desiredConnectionId))){"unknown connection in client"}
            require(previous.id == Identifier(msg.desiredConnectionId)){"mismatch connection"}
        }
        require(host.clientIds.contains(client.id)){"unknown client"}
        require(Identifier(msg.clientId) == client.id){"mismatch client"}

        require(msg.counterpartyChosenConnectionId == "" ||
                msg.counterpartyChosenConnectionId == msg.desiredConnectionId)

        require(previous == null ||
                (previous.end.state == Connection.State.STATE_INIT &&
                        previous.end.counterparty.connectionId == msg.counterparty.connectionId &&
                        previous.end.counterparty.prefix == msg.counterparty.prefix &&
                        previous.end.clientId == msg.clientId &&
                        previous.end.counterparty.clientId == msg.counterparty.clientId)
        ){"invalid previous state"}

        val versionsIntersection = msg.counterpartyVersionsList.intersect(if (previous != null) { previous.end.versionsList } else { host.getCompatibleVersions() })
        val version = host.pickVersion(versionsIntersection)

        val expected = Connection.ConnectionEnd.newBuilder()
                .setClientId(msg.counterparty.clientId)
                .addAllVersions(msg.counterpartyVersionsList)
                .setState(Connection.State.STATE_INIT)
                .setCounterparty(Connection.Counterparty.newBuilder()
                        .setClientId(msg.clientId)
                        .setConnectionId(msg.counterpartyChosenConnectionId)
                        .setPrefix(host.getCommitmentPrefix())
                        .build())
                .build()
        require(client.verifyConnectionState(
                msg.proofHeight,
                msg.counterparty.prefix,
                CommitmentProof(msg.proofInit),
                Identifier(msg.counterparty.connectionId),
                expected)){"connection verification failure"}

        val expectedConsensusState = host.getConsensusState(msg.consensusHeight)
        require(client.verifyClientConsensusState(
                msg.proofHeight,
                msg.counterparty.prefix,
                CommitmentProof(msg.proofConsensus),
                Identifier(msg.counterparty.clientId),
                msg.consensusHeight,
                expectedConsensusState)){"client consensus verification failure"}

        val identifier = msg.desiredConnectionId
        val connectionEnd = Connection.ConnectionEnd.newBuilder()
                .setClientId(msg.clientId)
                .addAllVersions(listOf(version))
                .setState(Connection.State.STATE_TRYOPEN)
                .setCounterparty(msg.counterparty)
                .build()
        ctx.addOutput(IbcConnection(host, Identifier(identifier), connectionEnd))
        ctx.addOutput(host.addConnection(Identifier(identifier)))
        ctx.addOutput(client.addConnection(Identifier(identifier)))
    }

    fun connOpenAck(ctx: Context, msg: Tx.MsgConnectionOpenAck) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getInput<IbcConnection>()

        require(host.clientIds.contains(client.id)){"unknown client"}
        require(host.connIds.contains(conn.id)){"unknown connection in host"}
        require(client.connIds.contains(conn.id)){"unknown connection in client"}
        require(conn.id == Identifier(msg.connectionId)){"mismatch connection"}
        require(client.id == Identifier(conn.end.clientId)){"mismatch client"}

        require(msg.consensusHeight <= host.getCurrentHeight()){"unknown height"}
        require(conn.end.counterparty.connectionId == "" ||
                msg.counterpartyConnectionId == conn.end.counterparty.connectionId)
        require(conn.end.state == Connection.State.STATE_INIT && conn.end.versionsList.contains(msg.version) ||
                conn.end.state == Connection.State.STATE_TRYOPEN || conn.end.versionsList.single() == msg.version){"invalid connection state"}

        val expected = Connection.ConnectionEnd.newBuilder()
                .setClientId(conn.end.counterparty.clientId)
                .addAllVersions(listOf(msg.version))
                .setState(Connection.State.STATE_TRYOPEN)
                .setCounterparty(Connection.Counterparty.newBuilder()
                        .setClientId(conn.end.clientId)
                        .setConnectionId(msg.connectionId)
                        .setPrefix(host.getCommitmentPrefix())
                        .build())
                .build()
        require(client.verifyConnectionState(
                msg.proofHeight,
                conn.end.counterparty.prefix,
                CommitmentProof(msg.proofTry),
                Identifier(msg.counterpartyConnectionId),
                expected)){"connection verification failure"}

        val expectedConsensusState = host.getConsensusState(msg.consensusHeight)
        require(client.verifyClientConsensusState(
                msg.proofHeight,
                conn.end.counterparty.prefix,
                CommitmentProof(msg.proofConsensus),
                Identifier(conn.end.counterparty.clientId),
                msg.consensusHeight,
                expectedConsensusState)){"client consensus verification failure"}

        ctx.addOutput(conn.copy(end = conn.end.toBuilder()
                .setState(Connection.State.STATE_OPEN)
                .clearVersions()
                .addAllVersions(listOf(msg.version))
                .build()))
    }

    fun connOpenConfirm(ctx: Context, msg: Tx.MsgConnectionOpenConfirm) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getInput<IbcConnection>()

        require(host.clientIds.contains(client.id)){"unknown client"}
        require(host.connIds.contains(conn.id)){"unknown connection in host"}
        require(client.connIds.contains(conn.id)){"unknown connection in client"}
        require(conn.id == Identifier(msg.connectionId)){"mismatch connection"}
        require(client.id == Identifier(conn.end.clientId)){"mismatch client"}

        require(conn.end.state == Connection.State.STATE_TRYOPEN){"invalid connection state"}
        val expected = Connection.ConnectionEnd.newBuilder()
                .setClientId(conn.end.counterparty.clientId)
                .addAllVersions(conn.end.versionsList)
                .setState(Connection.State.STATE_OPEN)
                .setCounterparty(Connection.Counterparty.newBuilder()
                        .setClientId(conn.end.clientId)
                        .setConnectionId(msg.connectionId)
                        .setPrefix(host.getCommitmentPrefix())
                        .build())
                .build()
        require(client.verifyConnectionState(
                msg.proofHeight,
                conn.end.counterparty.prefix,
                CommitmentProof(msg.proofAck),
                Identifier(conn.end.counterparty.connectionId),
                expected)){"connection verification failure"}

        ctx.addOutput(conn.copy(end = conn.end.toBuilder()
                .setState(Connection.State.STATE_OPEN)
                .build()
        ))
    }

    fun chanOpenInit(
            ctx: Context,
            order: ChannelOrder,
            connectionHops: List<Identifier>,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: Connection.Version
    ) {
        // TODO: port authentication should be added somehow

        val host = ctx.getInput<Host>()
        val conn = ctx.getReference<IbcConnection>()

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
        ctx.addOutput(IbcChannel(host, portIdentifier, channelIdentifier, end))
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
            version: Connection.Version,
            counterpartyVersion: Connection.Version,
            proofInit: CommitmentProof,
            proofHeight: Height
    ) {
        val host = ctx.getInput<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getReference<IbcConnection>()
        val previous = ctx.getInputOrNull<IbcChannel>()

        if (previous != null) {
            require(host.portChanIds.contains(Pair(portIdentifier, channelIdentifier)))
            require(previous.portId == portIdentifier)
            require(previous.id == channelIdentifier)
        }
        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(client.connIds.contains(conn.id))
        require(conn.id == connectionHops.single())
        require(client.id == Identifier(conn.end.clientId))

        require(counterpartyChosenChannelIdentifer == Identifier("") ||
                counterpartyChosenChannelIdentifer == channelIdentifier)

        require(previous == null ||
                ( previous.end.state == ChannelState.INIT &&
                        previous.end.ordering == order &&
                        previous.end.counterpartyPortIdentifier == counterpartyPortIdentifier &&
                        previous.end.counterpartyChannelIdentifier == counterpartyChannelIdentifier &&
                        previous.end.connectionHops == connectionHops &&
                        previous.end.version == version))

        require(conn.end.state == Connection.State.STATE_OPEN)

        val expected = ChannelEnd(
                ChannelState.INIT,
                order,
                portIdentifier,
                counterpartyChosenChannelIdentifer,
                listOf(Identifier(conn.end.counterparty.connectionId)),
                counterpartyVersion)
        require(client.verifyChannelState(
                proofHeight,
                conn.end.counterparty.prefix,
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

        val chan = IbcChannel(host, portIdentifier, channelIdentifier, end)
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
            counterpartyVersion: Connection.Version,
            counterpartyChannelIdentifier: Identifier,
            proofTry: CommitmentProof,
            proofHeight: Height
    ) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getReference<IbcConnection>()
        val chan = ctx.getInput<IbcChannel>()

        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(client.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))
        require(chan.portId == portIdentifier)
        require(chan.id == channelIdentifier)
        require(client.id == Identifier(conn.end.clientId))

        require(chan.end.state == ChannelState.INIT || chan.end.state == ChannelState.TRYOPEN)

        require(chan.end.counterpartyChannelIdentifier == Identifier("") ||
                counterpartyChannelIdentifier == chan.end.counterpartyChannelIdentifier)

        require(conn.id == chan.end.connectionHops.single())
        require(conn.end.state == Connection.State.STATE_OPEN)

        val expected = ChannelEnd(
                ChannelState.TRYOPEN,
                chan.end.ordering,
                portIdentifier,
                channelIdentifier,
                listOf(Identifier(conn.end.counterparty.connectionId)),
                counterpartyVersion)
        require(client.verifyChannelState(
                proofHeight,
                conn.end.counterparty.prefix,
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
        val conn = ctx.getReference<IbcConnection>()
        val chan = ctx.getInput<IbcChannel>()

        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(client.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))
        require(chan.portId == portIdentifier)
        require(chan.id == channelIdentifier)
        require(client.id == Identifier(conn.end.clientId))

        require(chan.end.state == ChannelState.TRYOPEN)

        require(conn.id == chan.end.connectionHops.single())
        require(conn.end.state == Connection.State.STATE_OPEN)

        val expected = ChannelEnd(
                ChannelState.OPEN,
                chan.end.ordering,
                portIdentifier,
                channelIdentifier,
                listOf(Identifier(conn.end.counterparty.connectionId)),
                chan.end.version)
        require(client.verifyChannelState(
                proofHeight,
                conn.end.counterparty.prefix,
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
        val conn = ctx.getReference<IbcConnection>()
        val chan = ctx.getInput<IbcChannel>()

        require(host.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))
        require(chan.portId == portIdentifier)
        require(chan.id == channelIdentifier)

        require(chan.end.state != ChannelState.CLOSED)

        require(conn.id == chan.end.connectionHops.single())
        require(conn.end.state == Connection.State.STATE_OPEN)

        ctx.addOutput(chan.copy(end = chan.end.copy(state = ChannelState.CLOSED)))
    }

    fun chanCloseConfirm(
            ctx: Context,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            proofInit: CommitmentProof,
            proofHeight: Height
    ) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getReference<IbcConnection>()
        val chan = ctx.getInput<IbcChannel>()

        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(client.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))
        require(chan.portId == portIdentifier)
        require(chan.id == channelIdentifier)
        require(client.id == Identifier(conn.end.clientId))

        require(chan.end.state != ChannelState.CLOSED)

        require(conn.id == chan.end.connectionHops.single())
        require(conn.end.state == Connection.State.STATE_OPEN)

        val expected = ChannelEnd(
                ChannelState.CLOSED,
                chan.end.ordering,
                portIdentifier,
                channelIdentifier,
                listOf(Identifier(conn.end.counterparty.connectionId)),
                chan.end.version)
        require(client.verifyChannelState(
                proofHeight,
                conn.end.counterparty.prefix,
                proofInit,
                chan.end.counterpartyPortIdentifier,
                chan.end.counterpartyChannelIdentifier,
                expected))

        ctx.addOutput(chan.copy(end = chan.end.copy(state = ChannelState.CLOSED)))
    }

    fun sendPacket(ctx: Context, packet: Packet) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getReference<IbcConnection>()
        val chan = ctx.getInput<IbcChannel>()

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

        require(Identifier(conn.end.clientId) == client.id)
        val latestClientHeight = client.latestClientHeight()
        require(packet.timeoutHeight.isZero() || latestClientHeight < packet.timeoutHeight)

        require(packet.sequence == chan.nextSequenceSend)

        ctx.addOutput(chan.copy(
                nextSequenceSend = chan.nextSequenceSend + 1,
                packets = chan.packets + mapOf(packet.sequence to packet)))
    }

    fun recvPacket(
            ctx: Context,
            packet: Packet,
            proof: CommitmentProof,
            proofHeight: Height,
            acknowledgement: Acknowledgement
    ) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getReference<IbcConnection>()
        var chan = ctx.getInput<IbcChannel>()

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
        require(conn.end.state == Connection.State.STATE_OPEN)

        require(packet.timeoutHeight.isZero() || host.getCurrentHeight() < packet.timeoutHeight)
        require(packet.timeoutTimestamp.timestamp == 0L || host.currentTimestamp().timestamp < packet.timeoutTimestamp.timestamp)

        require(client.verifyPacketData(
                proofHeight,
                conn.end.counterparty.prefix,
                proof,
                packet.sourcePort,
                packet.sourceChannel,
                packet.sequence,
                packet))

        if (!acknowledgement.isEmpty() || chan.end.ordering == ChannelOrder.UNORDERED) {
            chan = chan.copy(acknowledgements = chan.acknowledgements + mapOf(packet.sequence to acknowledgement))
        }

        if (chan.end.ordering == ChannelOrder.ORDERED) {
            require(packet.sequence == chan.nextSequenceRecv)
            chan = chan.copy(nextSequenceRecv = chan.nextSequenceRecv + 1)
        }

        ctx.addOutput(chan)
    }

    fun acknowledgePacket(
            ctx: Context,
            packet: Packet,
            acknowledgement: Acknowledgement,
            proof: CommitmentProof,
            proofHeight: Height
    ) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getReference<IbcConnection>()
        var chan = ctx.getInput<IbcChannel>()

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
        require(conn.end.state == Connection.State.STATE_OPEN)

        require(chan.packets[packet.sequence] == packet)

        require(client.verifyPacketAcknowledgement(
                proofHeight,
                conn.end.counterparty.prefix,
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
        ctx.addOutput(chan)
    }
}