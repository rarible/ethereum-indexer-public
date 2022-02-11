package com.rarible.protocol.nft.api.e2e.data

import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenStandard
import scalether.domain.AddressFactory
import java.util.*

fun createToken(): Token {
    return Token(
        id = AddressFactory.create(),
        owner = AddressFactory.create(),
        name = UUID.randomUUID().toString(),
        symbol = UUID.randomUUID().toString(),
        status = arrayOf(ContractStatus.PENDING, ContractStatus.CONFIRMED).random(),
        features = (1..10).map {  TokenFeature.values().random() }.toSet(),
        standard = TokenStandard.values().random(),
        version = null
    )
}
