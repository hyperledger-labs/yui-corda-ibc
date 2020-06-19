package jp.datachain.cosmos.x.ibc.ics23_commitment.types

enum class KeyEncoding(val keyEncoding: Int) {
    // URL encoding
    URL(0),
    // Hex encoding
    HEX(1),
}