package com.rarible.protocol.order.core.model

import io.daonomic.rpc.domain.Binary
import scalether.util.Hash

fun id(value: String): Binary = Binary.apply(Hash.sha3(value.toByteArray(Charsets.ISO_8859_1)).take(4).toByteArray())

fun id32(value: String): Binary = Binary.apply(Hash.sha3(value.toByteArray(Charsets.ISO_8859_1)))

const val ADMIN_AUTO_REDUCE_TASK_TYPE = "ADMIN_AUTO_REDUCE_TASK"
