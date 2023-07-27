package com.rarible.protocol.order.api.misc

import org.springframework.data.mongodb.core.index.Index

val Index.indexName: String
    get() {
        return indexKeys.map { (key, value) -> "${key}_$value" }.joinToString("_")
    }
