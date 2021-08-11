package com.rarible.protocol.order.api.misc

fun setField(target: Any, fieldName: String, value: Any) {
    val field = target.javaClass.getDeclaredField(fieldName)
    field.isAccessible = true
    field.set(target, value)
}

