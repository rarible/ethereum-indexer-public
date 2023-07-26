package com.rarible.protocol.order.listener.service.descriptors.exchange.sudoswap

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["common.feature-flags.sudoswap-enabled"], havingValue = "true")
annotation class EnableSudoSwap
