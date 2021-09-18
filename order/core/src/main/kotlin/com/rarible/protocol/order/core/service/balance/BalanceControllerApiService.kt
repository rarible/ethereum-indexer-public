package com.rarible.protocol.order.core.service.balance

import com.rarible.protocol.dto.Erc20DecimalBalanceDto
import com.rarible.protocol.erc20.api.client.Erc20BalanceControllerApi
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import scalether.domain.Address

@Component
class BalanceControllerApiService(
    private val erc20BalanceControllerApi: Erc20BalanceControllerApi
) {
    suspend fun getBalance(contract: Address, owner: Address): Erc20DecimalBalanceDto? {
        return try {
            erc20BalanceControllerApi.getErc20Balance(contract.toString(), owner.toString()).awaitFirstOrNull()
        } catch (ex: WebClientResponseException) {
            if (ex.statusCode == HttpStatus.NOT_FOUND) {
                null
            } else {
                throw ex
            }
        }
    }
}
