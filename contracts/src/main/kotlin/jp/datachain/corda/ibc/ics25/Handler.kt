package jp.datachain.corda.ibc.ics25

import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.client.v1.Client
import ibc.core.client.v1.Client.Height
import ibc.core.client.v1.compareTo
import ibc.core.client.v1.isZero
import ibc.core.connection.v1.Connection
import ibc.core.connection.v1.Tx
import ibc.lightclients.localhost.v1.Localhost
import ibc.lightclients.solomachine.v1.Solomachine
import ibc.lightclients.tendermint.v1.Tendermint
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.clients.corda.CordaClientState
import jp.datachain.corda.ibc.clients.corda.CordaConsensusState
import jp.datachain.corda.ibc.grpc.Corda
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.states.IbcChannel
import jp.datachain.corda.ibc.states.IbcConnection
import jp.datachain.corda.ibc.types.Timestamp

object Handler {
    fun createClient(ctx: Context, msg: Client.MsgCreateClient) {
        val prevHost = ctx.getInput<Host>()
        when {
            msg.clientState.`is`(Corda.ClientState::class.java) -> {
                val id = Identifier(msg.clientId)
                val nextHost = prevHost.addClient(id)
                val consensusState = msg.consensusState.unpack(Corda.ConsensusState::class.java)!!
                val client = CordaClientState(prevHost, id, CordaConsensusState(consensusState))
                ctx.addOutput(nextHost)
                ctx.addOutput(client)
            }
            msg.clientState.`is`(Tendermint.ClientState::class.java) -> throw NotImplementedError()
            msg.clientState.`is`(Solomachine.ClientState::class.java) -> throw NotImplementedError()
            msg.clientState.`is`(Localhost.ClientState::class.java) -> throw NotImplementedError()
            else -> throw IllegalArgumentException()
        }
    }

    fun connOpenInit(ctx: Context, msg: Tx.MsgConnectionOpenInit) {
        val host = ctx.getInput<Host>().addConnection(Identifier(msg.connectionId))
        val client = ctx.getReference<ClientState>()

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
        ctx.addOutput(IbcConnection(host, Identifier(msg.connectionId), end))
    }

    fun connOpenTry(ctx: Context, msg: Tx.MsgConnectionOpenTry) {
        val host = ctx.getInput<Host>()
        val client = ctx.getReference<ClientState>()
        val previous = ctx.getInputOrNull<IbcConnection>()

        if (previous != null) {
            require(host.connIds.contains(Identifier(msg.desiredConnectionId))){"unknown connection in host"}
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
        client.verifyConnectionState(
                msg.proofHeight,
                msg.counterparty.prefix,
                CommitmentProof(msg.proofInit),
                Identifier(msg.counterparty.connectionId),
                expected)

        val expectedConsensusState = host.getConsensusState(msg.consensusHeight)
        client.verifyClientConsensusState(
                msg.proofHeight,
                Identifier(msg.counterparty.clientId),
                msg.consensusHeight,
                msg.counterparty.prefix,
                CommitmentProof(msg.proofConsensus),
                expectedConsensusState)

        val identifier = msg.desiredConnectionId
        val connectionEnd = Connection.ConnectionEnd.newBuilder()
                .setClientId(msg.clientId)
                .addAllVersions(listOf(version))
                .setState(Connection.State.STATE_TRYOPEN)
                .setCounterparty(msg.counterparty)
                .build()
        ctx.addOutput(IbcConnection(host, Identifier(identifier), connectionEnd))
        ctx.addOutput(host.addConnection(Identifier(identifier)))
    }

    fun connOpenAck(ctx: Context, msg: Tx.MsgConnectionOpenAck) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getInput<IbcConnection>()

        require(host.clientIds.contains(client.id)){"unknown client"}
        require(host.connIds.contains(conn.id)){"unknown connection in host"}
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
        client.verifyConnectionState(
                msg.proofHeight,
                conn.end.counterparty.prefix,
                CommitmentProof(msg.proofTry),
                Identifier(msg.counterpartyConnectionId),
                expected)

        val expectedConsensusState = host.getConsensusState(msg.consensusHeight)
        client.verifyClientConsensusState(
                msg.proofHeight,
                Identifier(conn.end.counterparty.clientId),
                msg.consensusHeight,
                conn.end.counterparty.prefix,
                CommitmentProof(msg.proofConsensus),
                expectedConsensusState)

        ctx.addOutput(conn.copy(end = conn.end.toBuilder()
                .setState(Connection.State.STATE_OPEN)
                .clearVersions()
                .addAllVersions(listOf(msg.version))
                .apply{counterpartyBuilder.connectionId = msg.counterpartyConnectionId}
                .build()))
    }

    fun connOpenConfirm(ctx: Context, msg: Tx.MsgConnectionOpenConfirm) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getInput<IbcConnection>()

        require(host.clientIds.contains(client.id)){"unknown client"}
        require(host.connIds.contains(conn.id)){"unknown connection in host"}
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
        client.verifyConnectionState(
                msg.proofHeight,
                conn.end.counterparty.prefix,
                CommitmentProof(msg.proofAck),
                Identifier(conn.end.counterparty.connectionId),
                expected)

        ctx.addOutput(conn.copy(end = conn.end.toBuilder()
                .setState(Connection.State.STATE_OPEN)
                .build()
        ))
    }

    fun chanOpenInit(
            ctx: Context,
            msg: ibc.core.channel.v1.Tx.MsgChannelOpenInit
    ) {
        // TODO: port authentication should be added somehow

        val host = ctx.getInput<Host>()
        val conn = ctx.getReference<IbcConnection>()

        require(host.connIds.contains(conn.id))

        require(conn.id == Identifier(msg.channel.connectionHopsList.single()))

        val end = ChannelOuterClass.Channel.newBuilder()
                .setState(ChannelOuterClass.State.STATE_INIT)
                .setOrdering(msg.channel.ordering)
                .setCounterparty(msg.channel.counterparty)
                .addAllConnectionHops(msg.channel.connectionHopsList)
                .setVersion(msg.channel.version)
                .build()

        ctx.addOutput(host.addPortChannel(Identifier(msg.portId), Identifier(msg.channelId)))
        ctx.addOutput(IbcChannel(host, Identifier(msg.portId), Identifier(msg.channelId), end))
    }

    fun chanOpenTry(
            ctx: Context,
            msg: ibc.core.channel.v1.Tx.MsgChannelOpenTry
    ) {
        val host = ctx.getInput<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getReference<IbcConnection>()
        val previous = ctx.getInputOrNull<IbcChannel>()

        if (previous != null) {
            require(host.portChanIds.contains(Pair(Identifier(msg.portId), Identifier(msg.desiredChannelId))))
            require(previous.portId == Identifier(msg.portId))
            require(previous.id == Identifier(msg.desiredChannelId))
        }
        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(conn.id == Identifier(msg.channel.connectionHopsList.single()))
        require(client.id == Identifier(conn.end.clientId))

        require(msg.counterpartyChosenChannelId == "" ||
                msg.counterpartyChosenChannelId == msg.desiredChannelId)

        require(previous == null ||
                ( previous.end.state == ChannelOuterClass.State.STATE_INIT &&
                        previous.end.ordering == msg.channel.ordering &&
                        previous.end.counterparty.portId == msg.channel.counterparty.portId &&
                        previous.end.counterparty.channelId == msg.channel.counterparty.channelId &&
                        previous.end.connectionHopsList == msg.channel.connectionHopsList &&
                        previous.end.version == msg.channel.version))

        require(conn.end.state == Connection.State.STATE_OPEN)

        val expected = ChannelOuterClass.Channel.newBuilder()
                .setState(ChannelOuterClass.State.STATE_INIT)
                .setOrdering(msg.channel.ordering)
                .setCounterparty(ChannelOuterClass.Counterparty.newBuilder()
                        .setPortId(msg.portId)
                        .setChannelId(msg.counterpartyChosenChannelId)
                        .build())
                .addAllConnectionHops(listOf(conn.end.counterparty.connectionId))
                .setVersion(msg.counterpartyVersion)
                .build()
        client.verifyChannelState(
                msg.proofHeight,
                conn.end.counterparty.prefix,
                CommitmentProof(msg.proofInit),
                Identifier(msg.channel.counterparty.portId),
                Identifier(msg.channel.counterparty.channelId),
                expected)

        val end = ChannelOuterClass.Channel.newBuilder()
                .setState(ChannelOuterClass.State.STATE_TRYOPEN)
                .setOrdering(msg.channel.ordering)
                .setCounterparty(msg.channel.counterparty)
                .addAllConnectionHops(msg.channel.connectionHopsList)
                .setVersion(msg.channel.version)
                .build()

        val chan = IbcChannel(host, Identifier(msg.portId), Identifier(msg.desiredChannelId), end)
        ctx.addOutput(host.addPortChannel(Identifier(msg.portId), Identifier(msg.desiredChannelId)))
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
            msg: ibc.core.channel.v1.Tx.MsgChannelOpenAck
    ) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getReference<IbcConnection>()
        val chan = ctx.getInput<IbcChannel>()

        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))
        require(chan.portId == Identifier(msg.portId))
        require(chan.id == Identifier(msg.channelId))
        require(client.id == Identifier(conn.end.clientId))

        require(chan.end.state == ChannelOuterClass.State.STATE_INIT || chan.end.state == ChannelOuterClass.State.STATE_TRYOPEN)

        require(chan.end.counterparty.channelId == "" ||
                msg.counterpartyChannelId == chan.end.counterparty.channelId)

        require(conn.id == Identifier(chan.end.connectionHopsList.single()))
        require(conn.end.state == Connection.State.STATE_OPEN)

        val expected = ChannelOuterClass.Channel.newBuilder()
                .setState(ChannelOuterClass.State.STATE_TRYOPEN)
                .setOrdering(chan.end.ordering)
                .setCounterparty(ChannelOuterClass.Counterparty.newBuilder()
                        .setPortId(msg.portId)
                        .setChannelId(msg.channelId)
                        .build())
                .addAllConnectionHops(listOf(conn.end.counterparty.connectionId))
                .setVersion(msg.counterpartyVersion)
                .build()
        client.verifyChannelState(
                msg.proofHeight,
                conn.end.counterparty.prefix,
                CommitmentProof(msg.proofTry),
                Identifier(chan.end.counterparty.portId),
                Identifier(chan.end.counterparty.channelId),
                expected)

        ctx.addOutput(chan.copy(end = chan.end.toBuilder()
                .setState(ChannelOuterClass.State.STATE_OPEN)
                .setVersion(msg.counterpartyVersion)
                .apply{counterpartyBuilder.channelId = msg.counterpartyChannelId}
                .build()
        ))
    }

    fun chanOpenConfirm(
            ctx: Context,
            msg: ibc.core.channel.v1.Tx.MsgChannelOpenConfirm
    ) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getReference<IbcConnection>()
        val chan = ctx.getInput<IbcChannel>()

        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))
        require(chan.portId == Identifier(msg.portId))
        require(chan.id == Identifier(msg.channelId))
        require(client.id == Identifier(conn.end.clientId))

        require(chan.end.state == ChannelOuterClass.State.STATE_TRYOPEN)

        require(conn.id == Identifier(chan.end.connectionHopsList.single()))
        require(conn.end.state == Connection.State.STATE_OPEN)

        val expected = ChannelOuterClass.Channel.newBuilder()
                .setState(ChannelOuterClass.State.STATE_OPEN)
                .setOrdering(chan.end.ordering)
                .setCounterparty(ChannelOuterClass.Counterparty.newBuilder()
                        .setPortId(msg.portId)
                        .setChannelId(msg.channelId)
                        .build())
                .addAllConnectionHops(listOf(conn.end.counterparty.connectionId))
                .setVersion(chan.end.version)
                .build()
        client.verifyChannelState(
                msg.proofHeight,
                conn.end.counterparty.prefix,
                CommitmentProof(msg.proofAck),
                Identifier(chan.end.counterparty.portId),
                Identifier(chan.end.counterparty.channelId),
                expected)

        ctx.addOutput(chan.copy(end = chan.end.toBuilder()
                .setState(ChannelOuterClass.State.STATE_OPEN)
                .build()
        ))
    }

    fun chanCloseInit(
            ctx: Context,
            msg: ibc.core.channel.v1.Tx.MsgChannelCloseInit
    ) {
        val host = ctx.getReference<Host>()
        val conn = ctx.getReference<IbcConnection>()
        val chan = ctx.getInput<IbcChannel>()

        require(host.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))
        require(chan.portId == Identifier(msg.portId))
        require(chan.id == Identifier(msg.channelId))

        require(chan.end.state != ChannelOuterClass.State.STATE_CLOSED)

        require(conn.id == Identifier(chan.end.connectionHopsList.single()))
        require(conn.end.state == Connection.State.STATE_OPEN)

        ctx.addOutput(chan.copy(end = chan.end.toBuilder()
                .setState(ChannelOuterClass.State.STATE_CLOSED)
                .build()
        ))
    }

    fun chanCloseConfirm(
            ctx: Context,
            msg: ibc.core.channel.v1.Tx.MsgChannelCloseConfirm
    ) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getReference<IbcConnection>()
        val chan = ctx.getInput<IbcChannel>()

        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))
        require(chan.portId == Identifier(msg.portId))
        require(chan.id == Identifier(msg.channelId))
        require(client.id == Identifier(conn.end.clientId))

        require(chan.end.state != ChannelOuterClass.State.STATE_CLOSED)

        require(conn.id == Identifier(chan.end.connectionHopsList.single()))
        require(conn.end.state == Connection.State.STATE_OPEN)

        val expected = ChannelOuterClass.Channel.newBuilder()
                .setState(ChannelOuterClass.State.STATE_CLOSED)
                .setOrdering(chan.end.ordering)
                .apply {
                    counterpartyBuilder.portId = msg.portId
                    counterpartyBuilder.channelId = msg.channelId
                }
                .addAllConnectionHops(listOf(conn.end.counterparty.connectionId))
                .setVersion(chan.end.version)
                .build()
        client.verifyChannelState(
                msg.proofHeight,
                conn.end.counterparty.prefix,
                CommitmentProof(msg.proofInit),
                Identifier(chan.end.counterparty.portId),
                Identifier(chan.end.counterparty.channelId),
                expected)

        ctx.addOutput(chan.copy(end = chan.end.toBuilder()
                .setState(ChannelOuterClass.State.STATE_CLOSED)
                .build()
        ))
    }

    fun sendPacket(ctx: Context, packet: ChannelOuterClass.Packet) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getReference<IbcConnection>()
        val chan = ctx.getInput<IbcChannel>()

        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))

        require(chan.end.state != ChannelOuterClass.State.STATE_CLOSED)

        require(Identifier(packet.sourcePort) == chan.portId)
        require(Identifier(packet.sourceChannel) == chan.id)
        require(packet.destinationPort == chan.end.counterparty.portId)
        require(packet.destinationChannel == chan.end.counterparty.channelId)

        require(conn.id == Identifier(chan.end.connectionHopsList.single()))

        require(Identifier(conn.end.clientId) == client.id)
        val latestClientHeight = client.getLatestHeight()
        require(packet.timeoutHeight.isZero() || latestClientHeight < packet.timeoutHeight)

        require(packet.sequence == chan.nextSequenceSend)

        ctx.addOutput(chan.copy(
                nextSequenceSend = chan.nextSequenceSend + 1,
                packets = chan.packets + mapOf(packet.sequence to packet)))
    }

    fun recvPacket(
            ctx: Context,
            packet: ChannelOuterClass.Packet,
            proof: CommitmentProof,
            proofHeight: Height,
            acknowledgement: ChannelOuterClass.Acknowledgement
    ) {
        val host = ctx.getReference<Host>()
        val client = ctx.getReference<ClientState>()
        val conn = ctx.getReference<IbcConnection>()
        var chan = ctx.getInput<IbcChannel>()

        require(host.clientIds.contains(client.id))
        require(host.connIds.contains(conn.id))
        require(host.portChanIds.contains(Pair(chan.portId, chan.id)))

        require(Identifier(packet.destinationPort) == chan.portId)
        require(Identifier(packet.destinationChannel) == chan.id)

        require(chan.end.state == ChannelOuterClass.State.STATE_OPEN)
        require(packet.sourcePort == chan.end.counterparty.portId)
        require(packet.sourceChannel == chan.end.counterparty.channelId)

        require(conn.id == Identifier(chan.end.connectionHopsList.single()))
        require(conn.end.state == Connection.State.STATE_OPEN)

        require(packet.timeoutHeight.isZero() || host.getCurrentHeight() < packet.timeoutHeight)
        require(packet.timeoutTimestamp == 0L || host.currentTimestamp() < Timestamp(packet.timeoutTimestamp))

        client.verifyPacketCommitment(
                proofHeight,
                conn.end.counterparty.prefix,
                proof,
                Identifier(packet.sourcePort),
                Identifier(packet.sourceChannel),
                packet.sequence,
                packet.toByteArray())

        chan = chan.copy(acknowledgements = chan.acknowledgements + mapOf(packet.sequence to acknowledgement))

        chan = when (chan.end.ordering) {
            ChannelOuterClass.Order.ORDER_UNORDERED -> {
                require(!chan.receipts.contains(packet.sequence))
                chan.copy(receipts = chan.receipts + packet.sequence)
            }
            ChannelOuterClass.Order.ORDER_ORDERED -> {
                require(packet.sequence == chan.nextSequenceRecv)
                chan.copy(nextSequenceRecv = chan.nextSequenceRecv + 1)
            }
            else -> throw IllegalArgumentException()
        }

        ctx.addOutput(chan)
    }

    fun acknowledgePacket(
            ctx: Context,
            packet: ChannelOuterClass.Packet,
            acknowledgement: ChannelOuterClass.Acknowledgement,
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

        require(Identifier(packet.sourcePort) == chan.portId)
        require(Identifier(packet.sourceChannel) == chan.id)

        require(chan.end.state == ChannelOuterClass.State.STATE_OPEN)

        require(packet.destinationPort == chan.end.counterparty.portId)
        require(packet.destinationChannel == chan.end.counterparty.channelId)

        require(conn.id == Identifier(chan.end.connectionHopsList.single()))
        require(conn.end.state == Connection.State.STATE_OPEN)

        require(chan.packets[packet.sequence] == packet)

        client.verifyPacketAcknowledgement(
                proofHeight,
                conn.end.counterparty.prefix,
                proof,
                Identifier(packet.destinationPort),
                Identifier(packet.destinationChannel),
                packet.sequence,
                acknowledgement)

        if (chan.end.ordering == ChannelOuterClass.Order.ORDER_ORDERED) {
            require(packet.sequence == chan.nextSequenceAck)
            chan = chan.copy(nextSequenceAck = chan.nextSequenceAck + 1)
        }

        chan = chan.copy(packets = chan.packets - packet.sequence)
        ctx.addOutput(chan)
    }
}