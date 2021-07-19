package ibc.core.client.v1

operator fun Client.Height.compareTo(other: Client.Height) =
        if (revisionNumber == other.revisionNumber)
            revisionHeight.compareTo(other.revisionHeight)
        else
            revisionNumber.compareTo(other.revisionNumber)

fun Client.Height.isZero() = this == Client.Height.getDefaultInstance()
