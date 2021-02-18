package jp.datachain.corda.ibc.ics20

import com.google.protobuf.ByteString
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.util.JsonFormat
import ibc.applications.transfer.v1.Transfer
import ibc.core.channel.v1.ChannelOuterClass

fun MessageOrBuilder.toJson() = ByteString.copyFromUtf8(JsonFormat.printer().print(this))!!

fun ByteString.toFungibleTokenPacketData() = Transfer.FungibleTokenPacketData.newBuilder().also{
    JsonFormat.parser().merge(toStringUtf8(), it)
}.build()!!

fun ByteString.toAcknowledgement() = ChannelOuterClass.Acknowledgement.newBuilder().also{
    JsonFormat.parser().merge(toStringUtf8(), it)
}.build()!!
