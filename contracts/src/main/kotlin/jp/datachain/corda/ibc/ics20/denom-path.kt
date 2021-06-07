package jp.datachain.corda.ibc.ics20

import jp.datachain.corda.ibc.ics24.Identifier

fun String.addPath(portId: Identifier, channelId: Identifier): String {
    val newPath = "${portId.id}/${channelId.id}"
    return if (this.isEmpty()) {
        newPath
    } else {
        "$newPath/$this"
    }
}

fun String.removePrefix(): String {
    val components = split('/', ignoreCase = false, limit = 3)
    require(components.size == 3)
    return components.last()
}

fun String.hasPrefixes(vararg prefixes: String): Boolean {
    val components = split('/')
    if (components.size < prefixes.size) {
        return false
    }
    prefixes.forEachIndexed { i, prefix ->
        if (components[i] != prefix) {
            return false
        }
    }
    return true
}