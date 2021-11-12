package com.rarible.protocol.order.core.continuation

interface Continuation {
    companion object {
        fun splitBy(str: String?, separator: String): Pair<String, String>? {
            if (str == null || str.isEmpty()) {
                return null
            }
            val index = str.indexOf(separator)
            if (index < 0) {
                return null
            }
            return str.substring(0, index) to str.substring(index + separator.length)
        }
    }
}
