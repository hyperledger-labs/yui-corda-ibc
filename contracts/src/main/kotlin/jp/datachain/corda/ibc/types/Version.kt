package jp.datachain.corda.ibc.types

sealed class Version {
    data class Single(val version: String) : Version()
    data class Multiple(val versions: List<String>) : Version()
}
