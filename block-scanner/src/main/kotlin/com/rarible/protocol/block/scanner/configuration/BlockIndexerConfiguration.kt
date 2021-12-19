package com.rarible.protocol.block.scanner.configuration

import com.rarible.blockchain.scanner.ethereum.EnableEthereumScanner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.springframework.beans.factory.annotation.Configurable

@FlowPreview
@Configurable
@EnableEthereumScanner
@ExperimentalCoroutinesApi
class BlockIndexerConfiguration
