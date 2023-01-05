package jp.datachain.corda.ibc.clients

import ibc.core.client.v1.Client

fun Client.Height.isZero(): Boolean {
    return revisionHeight == 0L && revisionNumber == 0L
}

operator fun Client.Height.compareTo(other: Client.Height): Int {
    val number = revisionNumber.compareTo(other.revisionNumber)
    return if (number == 0) {
        revisionHeight.compareTo(other.revisionNumber)
    } else {
        number
    }
}
