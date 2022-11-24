package com.rarible.protocol.order.listener.metrics.sudoswap

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class SudoSwapCreatePairEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.sudoswap.event.create_pair", tag("blockchain", blockchain.value))

class SudoSwapUpdateDeltaEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.sudoswap.event.update.delta", tag("blockchain", blockchain.value))

class SudoSwapDepositNftEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.sudoswap.event.deposit.nft", tag("blockchain", blockchain.value))

class SudoSwapUpdateFeeEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.sudoswap.event.update.fee", tag("blockchain", blockchain.value))

class SudoSwapInNftEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.sudoswap.event.in.nft", tag("blockchain", blockchain.value))

class SudoSwapOutNftEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.sudoswap.event.out.nft", tag("blockchain", blockchain.value))

class SudoSwapUpdateSpotPriceEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.sudoswap.event.update.price", tag("blockchain", blockchain.value))

class SudoSwapWithdrawNftEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.sudoswap.event.withdraw.nft", tag("blockchain", blockchain.value))

class WrapperSudoSwapMatchEventMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.wrapper.sudoswap.event.match", tag("blockchain", blockchain.value))
