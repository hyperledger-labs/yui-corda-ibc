package jp.datachain.corda.ibc.ics20

import com.google.protobuf.ByteString
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.util.JsonFormat
import ibc.applications.transfer.v2.Packet.FungibleTokenPacketData
import ibc.core.channel.v1.ChannelOuterClass
import net.corda.core.crypto.SecureHash
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun <T: MessageOrBuilder> T.toJson() = ByteString.copyFromUtf8(JsonFormat.printer()
    .preservingProtoFieldNames()
    .includingDefaultValueFields()
    .sortingMapKeys()
    .omittingInsignificantWhitespace()
    .print(this))!!

fun ByteString.toFungibleTokenPacketData() = FungibleTokenPacketData.newBuilder().also{
    JsonFormat.parser().merge(toStringUtf8(), it)
}.build()!!

fun ByteString.toAcknowledgement() = ChannelOuterClass.Acknowledgement.newBuilder().also{
    JsonFormat.parser().merge(toStringUtf8(), it)
}.build()!!

fun ChannelOuterClass.Packet.toCommitment(): ByteArray {
    val buf = ByteBuffer.allocate(1024)
        .order(ByteOrder.BIG_ENDIAN)
        .putLong(timeoutTimestamp)
        .putLong(timeoutHeight.revisionNumber)
        .putLong(timeoutHeight.revisionHeight)
        .put(SecureHash.sha256(data.toByteArray()).bytes)
    val arr = ByteArray(buf.position())
    buf.flip()
    buf.get(arr)
    return SecureHash.sha256(arr).bytes
}