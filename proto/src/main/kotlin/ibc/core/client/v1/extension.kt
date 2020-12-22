package ibc.core.client.v1

operator fun Client.Height.compareTo(other: Client.Height) =
        if (versionNumber == other.versionNumber)
            versionHeight.compareTo(other.versionHeight)
        else
            versionNumber.compareTo(other.versionNumber)

fun Client.Height.isZero() = this == Client.Height.getDefaultInstance()
