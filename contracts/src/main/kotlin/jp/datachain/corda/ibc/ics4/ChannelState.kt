package jp.datachain.corda.ibc.ics4

enum class ChannelState {
    INIT,
    TRYOPEN,
    OPEN,
    CLOSED,
}