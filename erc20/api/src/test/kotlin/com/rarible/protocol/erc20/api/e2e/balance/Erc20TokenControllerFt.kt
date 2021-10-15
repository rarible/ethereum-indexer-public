package com.rarible.protocol.erc20.api.e2e.balance

import com.rarible.core.contract.model.Erc20Token
import com.rarible.core.contract.model.Erc721Token
import com.rarible.ethereum.contract.repository.ContractRepository
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.Erc20IndexerApiErrorDto
import com.rarible.protocol.erc20.api.AbstractFt
import com.rarible.protocol.erc20.api.End2EndTest
import com.rarible.protocol.erc20.api.client.Erc20TokenControllerApi
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import scalether.domain.AddressFactory

@End2EndTest
internal class Erc20TokenControllerFt : AbstractFt() {

    @Autowired
    protected lateinit var contractRepository: ContractRepository

    protected lateinit var client: Erc20TokenControllerApi

    @BeforeEach
    fun beforeEach() {
        client = clientFactory.createErc20TokenApiClient(Blockchain.ETHEREUM.value)
    }

    @Test
    fun `get token`() = runBlocking<Unit> {
        val contract = Erc20Token(
            id = AddressFactory.create(),
            name = RandomStringUtils.randomAlphabetic(8),
            decimals = 2,
            symbol = null
        )
        contractRepository.save(contract)

        val token = client.getErc20TokenById(contract.id.toString())
            .awaitFirst()

        assertEquals(contract.id, token.id)
        assertEquals(contract.name, token.name)
        assertEquals(contract.symbol, token.symbol)
    }

    @Test
    fun `get token - token is not an Erc20 token`() = runBlocking<Unit> {
        val contract = Erc721Token(
            id = AddressFactory.create(),
            name = RandomStringUtils.randomAlphabetic(8),
            symbol = null
        )
        contractRepository.save(contract)

        val e = assertThrows(Erc20TokenControllerApi.ErrorGetErc20TokenById::class.java, Executable {
            client.getErc20TokenById(contract.id.toString()).block()
        })
        val error = e.on404 as Erc20IndexerApiErrorDto

        assertEquals(HttpStatus.NOT_FOUND.value(), error.status)
        assertEquals(Erc20IndexerApiErrorDto.Code.TOKEN_NOT_FOUND, error.code)
    }

    // TODO this test checking current logic for request with non-existing contract ID,
    // originally there should be some kind of error with 404
    @Test
    fun `get token - token not found`() = runBlocking<Unit> {
        val contractId = AddressFactory.create()
        val token = client.getErc20TokenById(contractId.toString()).awaitFirst()

        assertEquals(contractId, token.id)
        assertEquals("", token.name)
        assertEquals("", token.symbol)
    }

    @Test
    fun `get token - incorrect query params`() = runBlocking<Unit> {
        val e = assertThrows(Erc20TokenControllerApi.ErrorGetErc20TokenById::class.java, Executable {
            client.getErc20TokenById("not an address").block()
        })
        val error = e.on400

        assertEquals(HttpStatus.BAD_REQUEST.value(), error.status)
        assertEquals(Erc20IndexerApiErrorDto.Code.VALIDATION, error.code)
    }
}
