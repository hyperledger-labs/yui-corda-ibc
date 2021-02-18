package jp.datachain.corda.ibc.ics20

import com.google.protobuf.ByteString
import com.google.protobuf.util.JsonFormat
import ibc.applications.transfer.v1.Transfer

fun Transfer.FungibleTokenPacketData.toJson() = ByteString.copyFromUtf8(JsonFormat.printer().print(this))!!

fun ByteString.toFungibleTokenPacketData() = Transfer.FungibleTokenPacketData.newBuilder().also{
    JsonFormat.parser().merge(toStringUtf8(), it)
}.build()!!