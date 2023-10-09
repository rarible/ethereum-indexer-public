package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.protocol.order.api.form.OrderForm
import com.rarible.protocol.order.api.service.order.validation.OrderFormValidator
import com.rarible.protocol.order.core.exception.ValidationApiException
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.service.nft.NftCollectionApiService
import org.springframework.stereotype.Component

@Component
class TokenPauseOrderValidator(
    val nftCollectionApiService: NftCollectionApiService
) : OrderFormValidator {
    override suspend fun validate(form: OrderForm) {
        val token = if (form.make.type.nft) {
            form.make.type.token
        } else if (form.take.type.nft) {
            form.take.type.token
        } else {
            return
        }
        val collection = nftCollectionApiService.getNftCollectionById(token) ?: return
        if (collection.flags?.paused == true) {
            throw ValidationApiException("Collection $token is paused")
        }
    }
}
