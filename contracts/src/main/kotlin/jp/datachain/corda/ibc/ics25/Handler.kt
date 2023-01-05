package jp.datachain.corda.ibc.ics25

import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.client.v1.Client.Height
import ibc.core.client.v1.compareTo
import ibc.core.client.v1.isZero
import ibc.core.connection.v1.Connection
import ibc.lightclients.corda.v1.Corda
import ibc.lightclients.fabric.v1.Fabric
import ibc.lightclients.lcp.v1.Lcp
import ibc.lightclients.localhost.v1.Localhost
import ibc.lightclients.solomachine.v1.Solomachine
import ibc.lightclients.tendermint.v1.Tendermint
import jp.datachain.corda.ibc.clients.corda.CordaClientState
import jp.datachain.corda.ibc.clients.fabric.FabricClientState
import jp.datachain.corda.ibc.clients.lcp.LcpClientState
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics20.toCommitment
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.states.IbcChannel
import jp.datachain.corda.ibc.states.IbcClientState
import jp.datachain.corda.ibc.states.IbcConnection
import jp.datachain.corda.ibc.types.Timestamp
import ibc.core.channel.v1.Tx as ChannelTx
import ibc.core.client.v1.Tx as ClientTx
import ibc.core.connection.v1.Tx as ConnectionTx

object Handler {
    fun createClient(ctx: Context, msg: ClientTx.MsgCreateClient) {
        val host = ctx.getInput<Host>()
        val (clientState: ClientState, clientType: ClientType) = when {
            msg.clientState.`is`(Corda.ClientState::class.java) -> Pair(CordaClientState(msg.clientState, msg.consensusState), ClientType.CordaClient)
            msg.clientState.`is`(Fabric.ClientState::class.java) -> Pair(FabricClientState(msg.clientState, msg.consensusState), ClientType.FabricClient)
            msg.clientState.`is`(Lcp.ClientState::class.java) -> Pair(LcpClientState(msg.clientState, msg.consensusState), ClientType.LcpClient)
            msg.clientState.`is`(Tendermint.ClientState::class.java) -> throw NotImplementedError()
            msg.clientState.`is`(Solomachine.ClientState::class.java) -> throw NotImplementedError()
            msg.clientState.`is`(Localhost.ClientState::class.java) -> throw NotImplementedError()
            else -> throw IllegalArgumentException()
        }

        val (nextHost, clientId) = host.generateClientIdentifier(clientType)
        val client = IbcClientState(host, clientId, clientState.anyClientState, clientState.anyConsensusStates)
        ctx.addOutput(nextHost)
        ctx.addOutput(client)
    }

    fun connOpenInit(ctx: Context, msg: ConnectionTx.MsgConnectionOpenInit) {
        val host = ctx.getInput<Host>()

        val (nextHost, connectionId) = host.generateConnectionIdentifier()

        val client = ctx.getReference<IbcClientState>()
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
                .setDelayPeriod(msg.delayPeriod)
                .build()

        ctx.addOutput(nextHost)
        ctx.addOutput(IbcConnection(host, connectionId, end))
    }

    fun connOpenTry(ctx: Context, msg: ConnectionTx.MsgConnectionOpenTry) {
        val host = ctx.getInput<Host>()

        val client = ctx.getReference<IbcClientState>()
        require(client.id == Identifier(msg.clientId)){"mismatch client"}

        val previousVersions = mutableListOf<Connection.Version>()
        val (nextHost, connectionId) = if (msg.previousConnectionId.isNotEmpty()) {
            val previous = ctx.getInput<IbcConnection>()
            require(previous.id == Identifier(msg.previousConnectionId)){
                "mismatch connection"
            }
            require( previous.end.counterparty.connectionId == "" &&
                    previous.end.counterparty.prefix == msg.counterparty.prefix &&
                    previous.end.clientId == msg.clientId &&
                    previous.end.counterparty.clientId == msg.counterparty.clientId &&
                    previous.end.delayPeriod == msg.delayPeriod){
                "connection fields mismatch previous connection fields"
            }
            require(previous.end.state == Connection.State.STATE_INIT){
                "previous connection state is in state ${previous.end.state}, expected INIT"
            }
            previousVersions.addAll(previous.end.versionsList)
            Pair(host.copy(), previous.id)
        } else {
            host.generateConnectionIdentifier()
        }

        val selfHeight = host.getCurrentHeight()
        require(msg.consensusHeight == selfHeight) {
            // INFO: The implementation of ibc-go requires that consensusHeight < selfHeight, but Corda-IBC requires equality.
            "consensus height is not equal to the current block height (${msg.consensusHeight} != $selfHeight)"
        }

        val expectedConsensusState = host.getConsensusState(msg.consensusHeight)
        val expectedCounterparty = Connection.Counterparty.newBuilder()
                .setClientId(msg.clientId)
                .setConnectionId("")
                .setPrefix(host.getCommitmentPrefix())
                .build()
        val expectedConnection = Connection.ConnectionEnd.newBuilder()
                .setClientId(msg.counterparty.clientId)
                .addAllVersions(msg.counterpartyVersionsList)
                .setState(Connection.State.STATE_INIT)
                .setCounterparty(expectedCounterparty)
                .setDelayPeriod(msg.delayPeriod)
                .build()

        val supportedVersions = if (previousVersions.isEmpty()) {
            host.getCompatibleVersions()
        } else {
            previousVersions
        }

        val version = host.pickVersion(supportedVersions, msg.counterpartyVersionsList)

        val end = Connection.ConnectionEnd.newBuilder()
                .setClientId(msg.clientId)
                .addAllVersions(listOf(version))
                .setState(Connection.State.STATE_TRYOPEN)
                .setCounterparty(msg.counterparty)
                .setDelayPeriod(msg.delayPeriod)
                .build()

        client.impl.verifyConnectionState(
                msg.proofHeight,
                msg.counterparty.prefix,
                CommitmentProof(msg.proofInit),
                Identifier(msg.counterparty.connectionId),
                expectedConnection)

        client.impl.verifyClientState(
                msg.proofHeight,
                msg.counterparty.prefix,
                Identifier(msg.counterparty.clientId),
                CommitmentProof(msg.proofClient),
                msg.clientState)

        client.impl.verifyClientConsensusState(
                msg.proofHeight,
                Identifier(msg.counterparty.clientId),
                msg.consensusHeight,
                msg.counterparty.prefix,
                CommitmentProof(msg.proofConsensus),
                expectedConsensusState.anyConsensusState)

        ctx.addOutput(nextHost)
        ctx.addOutput(IbcConnection(host, connectionId, end))
    }

    fun connOpenAck(ctx: Context, msg: ConnectionTx.MsgConnectionOpenAck) {
        val host = ctx.getReference<Host>()

        val conn = ctx.getInput<IbcConnection>()
        require(conn.id == Identifier(msg.connectionId)){"mismatch connection"}

        val client = ctx.getReference<IbcClientState>()
        require(client.id == Identifier(conn.end.clientId)){"mismatch client"}

        val selfHeight = host.getCurrentHeight()
        require(msg.consensusHeight == selfHeight) {
            // INFO: The implementation of ibc-go requires that consensusHeight < selfHeight, but Corda-IBC requires equality.
            "consensus height is not equal to the current block height (${msg.consensusHeight} != $selfHeight)"
        }

        when (conn.end.state) {
            Connection.State.STATE_INIT -> {
                require(host.getCompatibleVersions().contains(msg.version)) {
                    "connection state is in INIT but the provided version is not supported ${msg.version}"
                }
            }
            Connection.State.STATE_TRYOPEN -> {
                require(conn.end.versionsList.single() == msg.version){
                    "connection state is in TRYOPEN but the provided version (${msg.version}) is not set in the previous connection versions ${conn.end.versionsList}"
                }
            }
            else -> throw IllegalArgumentException("connection state is not INIT or TRYOPEN (got ${conn.end.state})")
        }

        val expectedConsensusState = host.getConsensusState(msg.consensusHeight)
        val expectedCounterparty = Connection.Counterparty.newBuilder()
                .setClientId(conn.end.clientId)
                .setConnectionId(msg.connectionId)
                .setPrefix(host.getCommitmentPrefix())
                .build()
        val expectedConnection = Connection.ConnectionEnd.newBuilder()
                .setClientId(conn.end.counterparty.clientId)
                .addAllVersions(listOf(msg.version))
                .setState(Connection.State.STATE_TRYOPEN)
                .setCounterparty(expectedCounterparty)
                .setDelayPeriod(conn.end.delayPeriod)
                .build()

        client.impl.verifyConnectionState(
                msg.proofHeight,
                conn.end.counterparty.prefix,
                CommitmentProof(msg.proofTry),
                Identifier(msg.counterpartyConnectionId),
                expectedConnection)

        client.impl.verifyClientState(
                msg.proofHeight,
                conn.end.counterparty.prefix,
                Identifier(conn.end.counterparty.clientId),
                CommitmentProof(msg.proofClient),
                msg.clientState
        )

        client.impl.verifyClientConsensusState(
                msg.proofHeight,
                Identifier(conn.end.counterparty.clientId),
                msg.consensusHeight,
                conn.end.counterparty.prefix,
                CommitmentProof(msg.proofConsensus),
                expectedConsensusState.anyConsensusState)

        ctx.addOutput(conn.copy(end = conn.end.toBuilder()
                .setState(Connection.State.STATE_OPEN)
                .clearVersions()
                .addAllVersions(listOf(msg.version))
                .apply{counterpartyBuilder.connectionId = msg.counterpartyConnectionId}
                .build()))
    }

    fun connOpenConfirm(ctx: Context, msg: ConnectionTx.MsgConnectionOpenConfirm) {
        val host = ctx.getReference<Host>()

        val conn = ctx.getInput<IbcConnection>()
        require(conn.id == Identifier(msg.connectionId)){"mismatch connection"}

        val client = ctx.getReference<IbcClientState>()
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
        client.impl.verifyConnectionState(
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
            msg: ChannelTx.MsgChannelOpenInit
    ) {
        val host = ctx.getInput<Host>()

        val conn = ctx.getReference<IbcConnection>()
        require(conn.id == Identifier(msg.channel.connectionHopsList.single()))

        conn.end.versionsList.single().let { version ->
            require(version.featuresList.contains(msg.channel.ordering.name)) {
                "connection version $version does not support channel ordering: ${msg.channel.ordering}"
            }
        }

        // TODO: port authentication should be added somehow

        val (nextHost, channelId) = host.generateChannelIdentifier()
        val end = ChannelOuterClass.Channel.newBuilder()
                .setState(ChannelOuterClass.State.STATE_INIT)
                .setOrdering(msg.channel.ordering)
                .setCounterparty(msg.channel.counterparty)
                .addAllConnectionHops(msg.channel.connectionHopsList)
                .setVersion(msg.channel.version)
                .build()

        ctx.addOutput(nextHost)
        ctx.addOutput(IbcChannel(host, Identifier(msg.portId), channelId, end))
    }

    fun chanOpenTry(
            ctx: Context,
            msg: ChannelTx.MsgChannelOpenTry
    ) {
        val host = ctx.getInput<Host>()

        val conn = ctx.getReference<IbcConnection>()
        require(conn.id == Identifier(msg.channel.connectionHopsList.single()))

        val client = ctx.getReference<IbcClientState>()
        require(client.id == Identifier(conn.end.clientId))

        var previous: IbcChannel? = null
        val (nextHost, channelId) = if (msg.previousChannelId.isNotEmpty()) {
            previous = ctx.getInput()
            require(previous.id == Identifier(msg.previousChannelId)){
                "mismatch channel"
            }
            require(previous.end.ordering == msg.channel.ordering &&
                    previous.end.counterparty.portId == msg.channel.counterparty.portId &&
                    previous.end.counterparty.channelId == "" &&
                    previous.end.connectionHopsList.first() == msg.channel.connectionHopsList.first() &&
                    previous.end.version == msg.channel.version) {
                "channel fields mismatch previous channel fields"
            }
            require(previous.end.state == ChannelOuterClass.State.STATE_INIT) {
                "previous channel state is in ${previous.end.state}, expected INIT"
            }
            Pair(host.copy(), previous.id)
        } else {
            host.generateChannelIdentifier()
        }

        // TODO: port authentication should be added somehow

        require(conn.end.state == Connection.State.STATE_OPEN) {
            "connection state is not OPEN (got ${conn.end.state})"
        }

        conn.end.versionsList.single().let{version ->
            require(version.featuresList.contains(msg.channel.ordering.name)) {
                "connection version $version does not support channel ordering: ${msg.channel.ordering}"
            }
        }

        val end = ChannelOuterClass.Channel.newBuilder()
                .setState(ChannelOuterClass.State.STATE_TRYOPEN)
                .setOrdering(msg.channel.ordering)
                .setCounterparty(msg.channel.counterparty)
                .addAllConnectionHops(msg.channel.connectionHopsList)
                .setVersion(msg.channel.version)
                .build()

        val counterpartyHops = listOf(conn.end.counterparty.connectionId)

        val expectedCounterparty = ChannelOuterClass.Counterparty.newBuilder()
                .setPortId(msg.portId)
                .setChannelId("")
                .build()
        val expectedChannel = ChannelOuterClass.Channel.newBuilder()
                .setState(ChannelOuterClass.State.STATE_INIT)
                .setOrdering(msg.channel.ordering)
                .setCounterparty(expectedCounterparty)
                .addAllConnectionHops(counterpartyHops)
                .setVersion(msg.counterpartyVersion)
                .build()

        client.impl.verifyChannelState(
                msg.proofHeight,
                conn.end.counterparty.prefix,
                CommitmentProof(msg.proofInit),
                Identifier(msg.channel.counterparty.portId),
                Identifier(msg.channel.counterparty.channelId),
                expectedChannel)

        val chan = IbcChannel(host, Identifier(msg.portId), channelId, end)
        ctx.addOutput(nextHost)
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
            msg: ChannelTx.MsgChannelOpenAck
    ) {
        val host = ctx.getReference<Host>()

        val chan = ctx.getInput<IbcChannel>()
        require(chan.portId == Identifier(msg.portId))
        require(chan.id == Identifier(msg.channelId))

        val conn = ctx.getReference<IbcConnection>()
        require(conn.id == Identifier(chan.end.connectionHopsList.single()))

        val client = ctx.getReference<IbcClientState>()
        require(client.id == Identifier(conn.end.clientId))

        require(chan.end.state == ChannelOuterClass.State.STATE_INIT ||
                chan.end.state == ChannelOuterClass.State.STATE_TRYOPEN) {
            "channel state should be INIT or TRYOPEN (got ${chan.end.state})"
        }

        // TODO: port authentication should be added somehow

        require(conn.end.state == Connection.State.STATE_OPEN) {
            "connection state is not OPEN (got ${conn.end.state})"
        }

        val counterpartyHops = listOf(conn.end.counterparty.connectionId)

        val expectedCounterparty = ChannelOuterClass.Counterparty.newBuilder()
                .setPortId(msg.portId)
                .setChannelId(msg.channelId)
                .build()
        val expectedChannel = ChannelOuterClass.Channel.newBuilder()
                .setState(ChannelOuterClass.State.STATE_TRYOPEN)
                .setOrdering(chan.end.ordering)
                .setCounterparty(expectedCounterparty)
                .addAllConnectionHops(counterpartyHops)
                .setVersion(msg.counterpartyVersion)
                .build()

        client.impl.verifyChannelState(
                msg.proofHeight,
                conn.end.counterparty.prefix,
                CommitmentProof(msg.proofTry),
                Identifier(chan.end.counterparty.portId),
                Identifier(msg.counterpartyChannelId),
                expectedChannel)

        ctx.addOutput(chan.copy(end = chan.end.toBuilder()
                .setState(ChannelOuterClass.State.STATE_OPEN)
                .setVersion(msg.counterpartyVersion)
                .apply{counterpartyBuilder.channelId = msg.counterpartyChannelId}
                .build()))
    }

    fun chanOpenConfirm(
            ctx: Context,
            msg: ChannelTx.MsgChannelOpenConfirm
    ) {
        val host = ctx.getReference<Host>()

        val chan = ctx.getInput<IbcChannel>()
        require(chan.portId == Identifier(msg.portId))
        require(chan.id == Identifier(msg.channelId))

        val conn = ctx.getReference<IbcConnection>()
        require(conn.id == Identifier(chan.end.connectionHopsList.single()))

        val client = ctx.getReference<IbcClientState>()
        require(client.id == Identifier(conn.end.clientId))

        require(chan.end.state == ChannelOuterClass.State.STATE_TRYOPEN) {
            "channel state is not TRYOPEN (got ${chan.end.state})"
        }

        // TODO: port authentication should be added somehow

        require(conn.end.state == Connection.State.STATE_OPEN) {
            "connection state is not OPEN (got ${conn.end.state})"
        }

        val counterpartyHops = listOf(conn.end.counterparty.connectionId)

        val expectedCounterparty = ChannelOuterClass.Counterparty.newBuilder()
                .setPortId(msg.portId)
                .setChannelId(msg.channelId)
                .build()
        val expectedChannel = ChannelOuterClass.Channel.newBuilder()
                .setState(ChannelOuterClass.State.STATE_OPEN)
                .setOrdering(chan.end.ordering)
                .setCounterparty(expectedCounterparty)
                .addAllConnectionHops(counterpartyHops)
                .setVersion(chan.end.version)
                .build()

        client.impl.verifyChannelState(
                msg.proofHeight,
                conn.end.counterparty.prefix,
                CommitmentProof(msg.proofAck),
                Identifier(chan.end.counterparty.portId),
                Identifier(chan.end.counterparty.channelId),
                expectedChannel)

        ctx.addOutput(chan.copy(end = chan.end.toBuilder()
                .setState(ChannelOuterClass.State.STATE_OPEN)
                .build()
        ))
    }

    fun chanCloseInit(
            ctx: Context,
            msg: ChannelTx.MsgChannelCloseInit
    ) {
        val host = ctx.getReference<Host>()

        val chan = ctx.getInput<IbcChannel>()
        require(chan.portId == Identifier(msg.portId))
        require(chan.id == Identifier(msg.channelId))

        val conn = ctx.getReference<IbcConnection>()
        require(conn.id == Identifier(chan.end.connectionHopsList.single()))

        require(chan.end.state != ChannelOuterClass.State.STATE_CLOSED) {
            "channel is already CLOSED"
        }

        require(conn.end.state == Connection.State.STATE_OPEN) {
            "connection state is not OPEN (got ${conn.end.state})"
        }

        ctx.addOutput(chan.copy(end = chan.end.toBuilder()
                .setState(ChannelOuterClass.State.STATE_CLOSED)
                .build()))
    }

    fun chanCloseConfirm(
            ctx: Context,
            msg: ChannelTx.MsgChannelCloseConfirm
    ) {
        val host = ctx.getReference<Host>()

        val chan = ctx.getInput<IbcChannel>()
        require(chan.portId == Identifier(msg.portId))
        require(chan.id == Identifier(msg.channelId))

        val conn = ctx.getReference<IbcConnection>()
        require(conn.id == Identifier(chan.end.connectionHopsList.single()))

        val client = ctx.getReference<IbcClientState>()
        require(client.id == Identifier(conn.end.clientId))

        // TODO: port authentication should be added somehow

        require(chan.end.state != ChannelOuterClass.State.STATE_CLOSED) {
            "channel is already CLOSED"
        }

        require(conn.end.state == Connection.State.STATE_OPEN) {
            "connection state is not OPEN (got ${conn.end.state})"
        }

        val counterpartyHops = listOf(conn.end.counterparty.connectionId)

        val expectedCounterparty = ChannelOuterClass.Counterparty.newBuilder()
                .setPortId(msg.portId)
                .setChannelId(msg.channelId)
                .build()
        val expectedChannel = ChannelOuterClass.Channel.newBuilder()
                .setState(ChannelOuterClass.State.STATE_CLOSED)
                .setOrdering(chan.end.ordering)
                .setCounterparty(expectedCounterparty)
                .addAllConnectionHops(counterpartyHops)
                .setVersion(chan.end.version)
                .build()

        client.impl.verifyChannelState(
                msg.proofHeight,
                conn.end.counterparty.prefix,
                CommitmentProof(msg.proofInit),
                Identifier(chan.end.counterparty.portId),
                Identifier(chan.end.counterparty.channelId),
                expectedChannel)

        ctx.addOutput(chan.copy(end = chan.end.toBuilder()
                .setState(ChannelOuterClass.State.STATE_CLOSED)
                .build()))
    }

    fun sendPacket(ctx: Context, packet: ChannelOuterClass.Packet) {
        val host = ctx.getReference<Host>()

        val chan = ctx.getInput<IbcChannel>()
        require(chan.portId == Identifier(packet.sourcePort))
        require(chan.id == Identifier(packet.sourceChannel))

        val conn = ctx.getReference<IbcConnection>()
        require(conn.id == Identifier(chan.end.connectionHopsList.single()))

        val client = ctx.getReference<IbcClientState>()
        require(client.id == Identifier(conn.end.clientId))

        require(chan.end.state != ChannelOuterClass.State.STATE_CLOSED) {
            "channel is CLOSED (got ${chan.end.state})"
        }

        // TODO: port authentication should be added somehow

        require(packet.destinationPort == chan.end.counterparty.portId) {
            "packet destination port doesn't match the counterparty's port (${packet.destinationPort} ≠ ${chan.end.counterparty.portId})"
        }
        require(packet.destinationChannel == chan.end.counterparty.channelId) {
            "packet destination channel doesn't match the counterparty's channel (${packet.destinationChannel} ≠ ${chan.end.counterparty.channelId})"
        }

        val latestHeight = client.impl.getLatestHeight()
        val timeoutHeight = packet.timeoutHeight
        require(packet.timeoutHeight.isZero() || latestHeight < timeoutHeight) {
            "receiving chain block height >= packet timeout height ($latestHeight >= $timeoutHeight)"
        }

        val (clientType, _) = host.parseClientIdentifier(Identifier(conn.end.clientId))
        if (clientType != ClientType.SoloMachineClient && clientType != ClientType.CordaClient) {
            val latestTimestamp = client.impl.consensusStates[latestHeight]!!.getTimestamp().timestamp

            require(packet.timeoutTimestamp == 0L || latestTimestamp < packet.timeoutTimestamp) {
                "receiving chain block timestamp >= packet timeout timestamp ($latestTimestamp >= ${packet.timeoutTimestamp})"
            }
        }

        require(packet.sequence == chan.nextSequenceSend) {
            "packet sequence ≠ next send sequence (${packet.sequence} ≠ ${chan.nextSequenceSend})"
        }

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

        var chan = ctx.getInput<IbcChannel>()
        require(chan.portId == Identifier(packet.destinationPort))
        require(chan.id == Identifier(packet.destinationChannel))

        val conn = ctx.getReference<IbcConnection>()
        require(conn.id == Identifier(chan.end.connectionHopsList.single()))

        val client = ctx.getReference<IbcClientState>()
        require(client.id == Identifier(conn.end.clientId))

        require(chan.end.state == ChannelOuterClass.State.STATE_OPEN) {
            "channel state is not OPEN (got ${chan.end.state})"
        }

        // TODO: port authentication should be added somehow

        require(packet.sourcePort == chan.end.counterparty.portId) {
            "packet source port doesn't match the counterparty's port (${packet.sourcePort} ≠ ${chan.end.counterparty.portId})"
        }
        require(packet.sourceChannel == chan.end.counterparty.channelId) {
            "packet source channel doesn't match the counterparty's channel (${packet.sourceChannel} ≠ ${chan.end.counterparty.channelId})"
        }

        require(conn.end.state == Connection.State.STATE_OPEN) {
            "connection state is not OPEN (got ${conn.end.state})"
        }

        val selfHeight = host.getCurrentHeight()
        val timeoutHeight = packet.timeoutHeight
        require(packet.timeoutHeight.isZero() || selfHeight < timeoutHeight) {
            "block height >= packet timeout height ($selfHeight >= $timeoutHeight)"
        }

        require(packet.timeoutTimestamp == 0L || host.currentTimestamp() < Timestamp(packet.timeoutTimestamp)) {
            "block timestamp >= packet timeout timestamp (${host.currentTimestamp()} >= ${Timestamp(packet.timeoutTimestamp)})"
        }

        client.impl.verifyPacketCommitment(
                proofHeight,
                conn.end.delayPeriod,
                0,
                conn.end.counterparty.prefix,
                proof,
                Identifier(packet.sourcePort),
                Identifier(packet.sourceChannel),
                packet.sequence,
                packet.toCommitment())

        chan = chan.copy(acknowledgements = chan.acknowledgements + mapOf(packet.sequence to acknowledgement))

        chan = when (chan.end.ordering) {
            ChannelOuterClass.Order.ORDER_UNORDERED -> {
                require(!chan.receipts.contains(packet.sequence)) {
                    "packet sequence (${packet.sequence})"
                }
                chan.copy(receipts = chan.receipts + packet.sequence)
            }
            ChannelOuterClass.Order.ORDER_ORDERED -> {
                require(packet.sequence == chan.nextSequenceRecv) {
                    if (packet.sequence < chan.nextSequenceRecv) {
                        "packet sequence (${packet.sequence}), next sequence receive (${chan.nextSequenceRecv})"
                    } else {
                        "packet sequence ≠ next receive sequence (${packet.sequence} ≠ ${chan.nextSequenceRecv})"
                    }
                }
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

        var chan = ctx.getInput<IbcChannel>()
        require(chan.portId == Identifier(packet.sourcePort))
        require(chan.id == Identifier(packet.sourceChannel))

        val conn = ctx.getReference<IbcConnection>()
        require(conn.id == Identifier(chan.end.connectionHopsList.single()))

        val client = ctx.getReference<IbcClientState>()
        require(client.id == Identifier(conn.end.clientId))

        require(chan.end.state == ChannelOuterClass.State.STATE_OPEN) {
            "channel state is not OPEN (got ${chan.end.state})"
        }

        // TODO: port authentication should be added somehow

        require(packet.destinationPort == chan.end.counterparty.portId) {
            "packet destination port doesn't match the counterparty's port (${packet.destinationPort} ≠ ${chan.end.counterparty.portId})"
        }
        require(packet.destinationChannel == chan.end.counterparty.channelId) {
            "packet destination channel doesn't match the counterparty's channel (${packet.destinationChannel} ≠ ${chan.end.counterparty.channelId})"
        }

        require(conn.end.state == Connection.State.STATE_OPEN) {
            "connection state is not OPEN (got ${conn.end.state})"
        }

        require(chan.packets.contains(packet.sequence)) {
            "packet with sequence (${packet.sequence}) has been acknowledged, or timed out. In rare cases, the packet referenced was never sent, likely due to the relayer being misconfigured"
        }
        val expectedPacket = chan.packets[packet.sequence]!!
        require(expectedPacket == packet) {
            "commitment bytes are not equal: got ($packet), expected ($expectedPacket)"
        }

        client.impl.verifyPacketAcknowledgement(
                proofHeight,
                conn.end.delayPeriod,
                0,
                conn.end.counterparty.prefix,
                proof,
                Identifier(packet.destinationPort),
                Identifier(packet.destinationChannel),
                packet.sequence,
                acknowledgement)

        if (chan.end.ordering == ChannelOuterClass.Order.ORDER_ORDERED) {
            require(packet.sequence == chan.nextSequenceAck) {
                "packet sequence ≠ next ack sequence (${packet.sequence} ≠ ${chan.nextSequenceAck})"
            }
            chan = chan.copy(nextSequenceAck = chan.nextSequenceAck + 1)
        }

        chan = chan.copy(packets = chan.packets - packet.sequence)
        ctx.addOutput(chan)
    }
}
