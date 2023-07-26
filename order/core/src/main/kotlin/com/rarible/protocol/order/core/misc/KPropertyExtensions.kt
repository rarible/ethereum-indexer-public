package com.rarible.protocol.order.core.misc

import org.springframework.data.mongodb.core.query.KPropertyPath
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

@Suppress("UNCHECKED_CAST")
operator fun <T, O : T, U> KProperty<T>.div(other: KProperty1<O, U>) =
    KPropertyPath(this, other as KProperty1<T, U>)
