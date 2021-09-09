package com.rarible.protocol.order.core.repository.sort

enum class OrderActivitySort {
    LATEST_FIRST,
    EARLIEST_FIRST;

    companion object {
        fun fromString(value: String?) = if (value == EARLIEST_FIRST.name) EARLIEST_FIRST else LATEST_FIRST
    }
}