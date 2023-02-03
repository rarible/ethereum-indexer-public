package com.rarible.protocol.order.core.parser

import com.rarible.protocol.contracts.seaport.v1.events.OrderFulfilledEvent
import com.rarible.protocol.order.core.data.log
import com.rarible.protocol.order.core.model.Order
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SeaportOrderParserTest {
    @Test
    fun `convert - advanced order`() {
        //
        val advanced = SeaportOrderParser.parseAdvancedOrders(matchAdvancedOrdersTransaction)
        assertThat(advanced).hasSize(2)

        val firstOrderMatch = log(
            topics = listOf(
                Word.apply("0x9d9af8e38d66c62e2c12f0225249fd9d721c54b83f48d9352c97c6cacdcb6f31"),
                Word.apply("0x0000000000000000000000000228aca40362f58092c3dc6c3b5e23690f91e37f"),
                Word.apply("0x000000000000000000000000004c00500000ad104d7dbd00e3ae0a5c00560c00"),
            ),
            data = firstAdvancedOrderLogData.prefixed()
        ).let { OrderFulfilledEvent.apply(it) }
        val secondOrderMatch = log(
            topics = listOf(
                Word.apply("0x9d9af8e38d66c62e2c12f0225249fd9d721c54b83f48d9352c97c6cacdcb6f31"),
                Word.apply("0x000000000000000000000000dddd34f88b475dae9fef76af218b00cca0d7a06a"),
                Word.apply("0x0000000000000000000000000000000000000000000000000000000000000000"),
            ),
            data = secondAdvancedOrderLogData.prefixed()
        ).let { OrderFulfilledEvent.apply(it) }

        assertThat(
            advanced[0].signature
        ).isEqualTo(Binary.apply("0x0c4df824ed82c780e20057cf9eb2190a58a5f6295aaa80713cef7a1fdd3d215e20752596d710c318312131785f72edd6a5b41aed613221d884ba673f922041a11c"))
        assertThat(
            advanced[0].parameters.offer.single().token
        ).isEqualTo(SeaportOrderParser.convert(firstOrderMatch.offer()).single().token)
        assertThat(
            Order.Companion.seaportV1Hash(advanced[0].parameters, 0)
        ).isEqualTo(Word.apply(firstOrderMatch.orderHash()))

        assertThat(
            advanced[1].signature
        ).isEqualTo(Binary.empty())
        assertThat(
            advanced[1].parameters.consideration.single().token
        ).isEqualTo(SeaportOrderParser.convert(secondOrderMatch.consideration()).single().token)
        assertThat(
            Order.seaportV1Hash(advanced[1].parameters, 0)
        ).isEqualTo(Word.apply(secondOrderMatch.orderHash()))
    }

    /**
     * https://etherscan.io/tx/0x68f832e9ae420637d778b3de4139e501ee806c215dfb51eb6600679c82a931d2
     */
    private val firstAdvancedOrderLogData = Binary.apply(
        "85a8463c38bed08f12151c0204753e822abd123378a4b9b18f56bbf923fb07e9" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000080" +
                "0000000000000000000000000000000000000000000000000000000000000120" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "000000000000000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc2" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000001a30ae66b41e0000" +
                "0000000000000000000000000000000000000000000000000000000000000003" +
                "0000000000000000000000000000000000000000000000000000000000000002" +
                "00000000000000000000000042069abfe407c60cf4ae4112bedead391dba1cdb" +
                "0000000000000000000000000000000000000000000000000000000000001206" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000228aca40362f58092c3dc6c3b5e23690f91e37f" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "000000000000000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc2" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000a79df5c480c000" +
                "0000000000000000000000000000a26b00c1f0df003000390027140000faa719" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "000000000000000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc2" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000014f3beb89018000" +
                "000000000000000000000000f14d484b29a8ac040feb489afadb4b972422b4e9"
    )
    private val secondAdvancedOrderLogData = Binary.apply(
        "316f27c6328205bac04dda61332e85b5e2b1708c993d42744d4fe3a6714bd067" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000080" +
                "0000000000000000000000000000000000000000000000000000000000000120" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000000000000000000000000000000002" +
                "00000000000000000000000042069abfe407c60cf4ae4112bedead391dba1cdb" +
                "0000000000000000000000000000000000000000000000000000000000001206" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "000000000000000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc2" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000001839d485669bc000" +
                "000000000000000000000000dddd34f88b475dae9fef76af218b00cca0d7a06a"
    )
    private val matchAdvancedOrdersTransaction = Binary.apply("0x55944a42000000000000000000000000000000000000000000" +
            "00000000000000000000600000000000000000000000000000000000000000000000000000000000000ac0000000000000000000000" +
            "0000000000000000000000000000000000000000d600000000000000000000000000000000000000000000000000000000000000002" +
            "00000000000000000000000000000000000000000000000000000000000000400000000000000000000000000000000000000000000" +
            "00000000000000000062000000000000000000000000000000000000000000000000000000000000000a00000000000000000000000" +
            "00000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000010" +
            "00000000000000000000000000000000000000000000000000000000000052000000000000000000000000000000000000000000000" +
            "000000000000000005a00000000000000000000000000228aca40362f58092c3dc6c3b5e23690f91e37f00000000000000000000000" +
            "0004c00500000ad104d7dbd00e3ae0a5c00560c00000000000000000000000000000000000000000000000000000000000000016000" +
            "00000000000000000000000000000000000000000000000000000000000220000000000000000000000000000000000000000000000" +
            "00000000000000000020000000000000000000000000000000000000000000000000000000063ce1e46000000000000000000000000" +
            "0000000000000000000000000000000063ce21c90000000000000000000000000000000000000000000000000000000000000000360" +
            "c6ebe00000000000000000000000000000000000000000637b335ac7bbd380000007b02230091a7ed01230072f7006a004d60a8d4e7" +
            "1d599b8104250f000000000000000000000000000000000000000000000000000000000000000000030000000000000000000000000" +
            "00000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000010000" +
            "00000000000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc200000000000000000000000000000000000000000000000" +
            "000000000000000000000000000000000000000000000000000000000000000001a30ae66b41e000000000000000000000000000000" +
            "00000000000000000000001a30ae66b41e0000000000000000000000000000000000000000000000000000000000000000000300000" +
            "0000000000000000000000000000000000000000000000000000000000400000000000000000000000042069abfe407c60cf4ae4112" +
            "bedead391dba1cdb49b9709ec16a2bf006a7b64d1af1f0ed7f28e890650602a2002357f38a9bfafe000000000000000000000000000" +
            "00000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001000000" +
            "0000000000000000000228aca40362f58092c3dc6c3b5e23690f91e37f0000000000000000000000000000000000000000000000000" +
            "000000000000001000000000000000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc20000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000a79df5c480c0000000000" +
            "0000000000000000000000000000000000000000000a79df5c480c0000000000000000000000000000000a26b00c1f0df0030003900" +
            "27140000faa7190000000000000000000000000000000000000000000000000000000000000001000000000000000000000000c02aa" +
            "a39b223fe8d0a0e5c4f27ead9083c756cc2000000000000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000000000000014f3beb89018000000000000000000000000000000000000000000000000000014" +
            "f3beb89018000000000000000000000000000f14d484b29a8ac040feb489afadb4b972422b4e9000000000000000000000000000000" +
            "00000000000000000000000000000000410c4df824ed82c780e20057cf9eb2190a58a5f6295aaa80713cef7a1fdd3d215e20752596d" +
            "710c318312131785f72edd6a5b41aed613221d884ba673f922041a11c00000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a00000000000" +
            "00000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000" +
            "0000000000100000000000000000000000000000000000000000000000000000000000003a000000000000000000000000000000000" +
            "000000000000000000000000000003e0000000000000000000000000dddd34f88b475dae9fef76af218b00cca0d7a06a00000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000001600000000000000000000000000000000000000000000000000000000000000220000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000063ce1e46000000000000" +
            "0000000000000000000000000000000000000000000063ce21c90000000000000000000000000000000000000000000000000000000" +
            "000000000360c6ebe000000000000000000000000000000000000000009c5004b83deb0360000007b02230091a7ed01230072f7006a" +
            "004d60a8d4e71d599b8104250f000000000000000000000000000000000000000000000000000000000000000000010000000000000" +
            "00000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000" +
            "0000000200000000000000000000000042069abfe407c60cf4ae4112bedead391dba1cdb00000000000000000000000000000000000" +
            "00000000000000000000000001206000000000000000000000000000000000000000000000000000000000000000100000000000000" +
            "00000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000" +
            "00000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000c02aaa39b223" +
            "fe8d0a0e5c4f27ead9083c756cc20000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "0000000000000000000000000000000001839d485669bc0000000000000000000000000000000000000000000000000001839d48566" +
            "9bc000000000000000000000000000dddd34f88b475dae9fef76af218b00cca0d7a06a0000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000" +
            "00000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000001206000000000000000000000000000000000000000" +
            "00000000000000000000000a0000000000000000000000000000000000000000000000000000000000000000d375af38c615bc3b651" +
            "d74d7d59974b82de2cfc350734166de4356711f3dca12904a9f8ea26b99a311e7f1f5296986baefce4343ab8876d1bc6fb8e2513661" +
            "da176dfadbe981215e8385eb0fd5078353789a3c1949703522b6cc8a5b978db156fa6e399c0097394e8ec1451f594281798c813f703" +
            "e50eb659e2060e0239dba2552f24cca8c6f5edccb07651ec0cb1c8913c080babc0cffda2ec39946326b54f81f1404b67429efd8651e" +
            "5ec98560acb3064e44271bb9ea538e50a77bfae51cbd721d0c3ad3a70835957c147c30ca3b508b287db767e799b1b4611ed7fab3da7" +
            "57a88c4bbb120024313cce10c222f030179e4c1e604eda0d13a381b8bfad53e00e8e557abd90e779c9def6b46e3d99c21ea0e127a48" +
            "0ea30619c29562b23e6325d2511f0111a609909fa0d7a138c5ddd792851e4a9a254e50142bfda4973d75c5b757324c8a0cf30d2256c" +
            "e1e4261b67c6882100ef3b2aa5698c6bee33ae06b066964182981eb0c3ab32c722b0eeeec67c8edc72a4b8024ce0c6ce43566161469" +
            "74ebfd7111b862536cc85dbe4ba9f50d2aa3748f0f8947f4aa25855e5a1b11151000000000000000000000000000000000000000000" +
            "00000000000000000000040000000000000000000000000000000000000000000000000000000000000080000000000000000000000" +
            "00000000000000000000000000000000000000001800000000000000000000000000000000000000000000000000000000000000280" +
            "00000000000000000000000000000000000000000000000000000000000003800000000000000000000000000000000000000000000" +
            "00000000000000000004000000000000000000000000000000000000000000000000000000000000000a00000000000000000000000" +
            "00000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000010" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004000" +
            "000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000000000000000000000000" +
            "00000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000100000000000000000000000000000000000000000000000000000000000000400000000000000000000000000" +
            "0000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000000010000" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000" +
            "00000000000000000000000000000000000000000000000000000000040000000000000000000000000000000000000000000000000" +
            "00000000000000a00000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
            "00000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000" +
            "000000000000001000000000000000000000000000000000000000000000000000000000000000000000000360c6ebe"
    )
    /**
     * End logs
     */
}