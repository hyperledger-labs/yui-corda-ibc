package jp.datachain.cosmos.x.ibc.types

enum class State(val state: Int) {
    // Default State
    UNINITIALIZED(0),
    // A channel or connection end has just started the opening handshake.
    INIT(1),
    // A channel or connection end has acknowledged the handshake step on the counterparty chain.
    TRYOPEN(2),
    // A channel or connection end has completed the handshake. Open channels are
    // ready to send and receive packets.
    OPEN(3),
    // A channel end has been closed and can no longer be used to send or receive packets.
    CLOSED(4)
}
