package jp.datachain.corda.ibc.clients.corda

import com.google.protobuf.ByteString
import ibc.core.client.v1.Client
import ibc.core.commitment.v1.Commitment
import ibc.core.connection.v1.Connection

val HEIGHT: Client.Height = Client.Height.newBuilder()
        .setVersionNumber(1)
        .setVersionHeight(1)
        .build()

val VERSION: Connection.Version = Connection.Version.newBuilder()
        .setIdentifier("1")
        .addAllFeatures(listOf("ORDER_ORDERED", "ORDER_UNORDERED"))
        .build()

val PREFIX: Commitment.MerklePrefix = Commitment.MerklePrefix.newBuilder()
        .setKeyPrefix(ByteString.copyFrom("ibc", Charsets.US_ASCII))
        .build()