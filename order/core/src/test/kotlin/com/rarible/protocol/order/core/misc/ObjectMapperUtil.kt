package com.rarible.protocol.order.core.misc

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

val MAPPER: ObjectMapper = ObjectMapper()
    .registerKotlinModule()
    .registerModules(JavaTimeModule())
    .registerModules()
