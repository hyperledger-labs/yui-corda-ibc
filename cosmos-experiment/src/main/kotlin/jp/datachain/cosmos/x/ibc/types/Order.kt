package jp.datachain.cosmos.x.ibc.types

import com.fasterxml.jackson.annotation.JsonValue

enum class Order(@JsonValue val order: Int) {
    // zero-value for channel ordering
    NONE(0),
    // packets can be delivered in any order, which may differ from the order in which they were sent.
    UNORDERED(1),
    // packets are delivered exactly in the order which they were sent
    ORDERED(2),
}