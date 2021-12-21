package com.rarible.protocol.nft.core.service.token.reduce

import com.rarible.core.entity.reducer.service.EntityTemplateProvider
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class TokenTemplateProvider : EntityTemplateProvider<Address, Token> {
    override fun getEntityTemplate(id: Address): Token {
        return Token(
            id = id,
            name = "",
            status = ContractStatus.PENDING,
            standard = TokenStandard.NONE
        )
    }
}
