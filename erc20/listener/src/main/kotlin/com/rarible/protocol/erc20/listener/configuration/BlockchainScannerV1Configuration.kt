package com.rarible.protocol.erc20.listener.configuration

import com.rarible.ethereum.listener.log.EnableLogListeners
import com.rarible.ethereum.monitoring.EnableBlockchainMonitoring
import com.rarible.protocol.erc20.listener.Erc20ListenerApplication
import org.springframework.context.annotation.Configuration

@Configuration
@EnableOnScannerV1
@EnableBlockchainMonitoring
@EnableLogListeners(scanPackage = [Erc20ListenerApplication::class])
class BlockchainScannerV1Configuration