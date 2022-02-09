package com.rarible.protocol.erc20.api.e2e.balance

import com.rarible.core.common.nowMillis
import com.rarible.core.contract.model.Erc20Token
import com.rarible.core.contract.model.Erc721Token
import com.rarible.ethereum.contract.repository.ContractRepository
import com.rarible.ethereum.domain.Blockchain
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.api.AbstractFt
import com.rarible.protocol.erc20.api.End2EndTest
import com.rarible.protocol.erc20.api.client.Erc20BalanceControllerApi
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.repository.Erc20BalanceRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.AddressFactory
import java.math.BigDecimal
import java.math.BigInteger

@End2EndTest
internal class Erc20BalanceControllerFt : AbstractFt() {

    @Autowired
    protected lateinit var erc20BalanceRepository: Erc20BalanceRepository

    @Autowired
    protected lateinit var contractRepository: ContractRepository

    protected lateinit var client: Erc20BalanceControllerApi

    @BeforeEach
    fun beforeEach() {
        client = clientFactory.createErc20BalanceApiClient(Blockchain.ETHEREUM.value)
    }

    @Test
    fun `get existing balance`() = runBlocking<Unit> {
        val erc20Balance = Erc20Balance(
            token = AddressFactory.create(),
            owner = AddressFactory.create(),
            balance = EthUInt256.TEN,
            createdAt = nowMillis(),
            lastUpdatedAt = nowMillis(),
        )
        erc20BalanceRepository.save(erc20Balance)

        val balanceDto = client.getErc20Balance(
            erc20Balance.token.toString(),
            erc20Balance.owner.toString()
        ).awaitFirst()

        assertEquals(balanceDto.contract, erc20Balance.token)
        assertEquals(balanceDto.owner, erc20Balance.owner)
        assertEquals(balanceDto.balance, erc20Balance.balance.value)
    }

    // TODO maybe use here 404 and Erc20BalanceNotFoundException?
    @Test
    fun `get not existing balance`() = runBlocking<Unit> {
        val token = AddressFactory.create()
        val owner = AddressFactory.create()

        val balanceDto = client.getErc20Balance(
            token.toString(),
            owner.toString()
        ).awaitFirst()

        assertEquals(balanceDto.contract, token)
        assertEquals(balanceDto.owner, owner)
        assertEquals(balanceDto.balance, BigInteger.ZERO)
        assertEquals(balanceDto.decimalBalance, BigDecimal.ZERO)
    }

    @Test
    fun `get with contract decimals - Erc20`() = runBlocking<Unit> {
        val contract = Erc20Token(
            id = AddressFactory.create(),
            name = RandomStringUtils.randomAlphabetic(8),
            decimals = 2,
            symbol = null
        )
        contractRepository.save(contract)

        val erc20Balance = Erc20Balance(
            token = contract.id,
            owner = AddressFactory.create(),
            balance = EthUInt256.of(1054),
            createdAt = nowMillis(),
            lastUpdatedAt = nowMillis()
        )
        erc20BalanceRepository.save(erc20Balance)

        val balanceDto = client.getErc20Balance(
            erc20Balance.token.toString(),
            erc20Balance.owner.toString()
        ).awaitFirst()

        assertEquals(balanceDto.contract, erc20Balance.token)
        assertEquals(balanceDto.owner, erc20Balance.owner)
        assertEquals(balanceDto.balance, erc20Balance.balance.value)
        assertEquals(balanceDto.decimalBalance, BigDecimal("10.54"))
    }

    @Test
    fun `get with contract decimals - not Erc20`() = runBlocking<Unit> {
        val contract = Erc721Token(
            id = AddressFactory.create(),
            name = RandomStringUtils.randomAlphabetic(8),
            symbol = null
        )
        contractRepository.save(contract)

        val erc20Balance = Erc20Balance(
            token = contract.id,
            owner = AddressFactory.create(),
            balance = EthUInt256.of(1054),
            createdAt = nowMillis(),
            lastUpdatedAt = nowMillis()
        )
        erc20BalanceRepository.save(erc20Balance)

        val balanceDto = client.getErc20Balance(
            erc20Balance.token.toString(),
            erc20Balance.owner.toString()
        ).awaitFirst()

        assertEquals(balanceDto.contract, erc20Balance.token)
        assertEquals(balanceDto.owner, erc20Balance.owner)
        assertEquals(balanceDto.balance, erc20Balance.balance.value)
        assertEquals(balanceDto.decimalBalance, erc20Balance.balance.value.toBigDecimal())
    }
}
