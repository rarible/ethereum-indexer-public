package com.rarible.protocol.erc20.core.model

import scalether.domain.Address

data class Wallet(
    val token: Address,
    val owner: Address
)
