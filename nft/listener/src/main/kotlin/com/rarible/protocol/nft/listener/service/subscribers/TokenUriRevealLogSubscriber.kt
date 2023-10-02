package com.rarible.protocol.nft.listener.service.subscribers

import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.core.model.TokenUriReveal
import com.rarible.protocol.nft.listener.service.descriptors.erc721.TokenUriRevealLogDescriptor
import org.springframework.stereotype.Component

@Component
class TokenUriRevealLogSubscriber(descriptor: TokenUriRevealLogDescriptor, autoReduceService: AutoReduceService) :
    AbstractItemLogEventSubscriber<TokenUriReveal>(SubscriberGroups.TOKEN_REVEAL, descriptor, autoReduceService)
