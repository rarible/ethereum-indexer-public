package com.rarible.protocol.nft.core.service.item.meta.properties

import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.service.item.meta.descriptors.getText
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JsonPropertiesParserTest {

    @Test
    fun `not a json`() {
        val data = "abc"
        assertThat(JsonPropertiesParser.parse(createRandomItemId(), data)).isNull()
    }

    @Test
    fun `regular json`() {
        val data = """{"a": "b"}"""
        val node = JsonPropertiesParser.parse(createRandomItemId(), data)!!

        assertThat(node.getText("a")).isEqualTo("b")
    }

    @Test
    fun `regular json with trailing spaces`() {
        val data = "\n\t\r " + """{"a": "b"}""" + "\n\t\r   "
        val node = JsonPropertiesParser.parse(createRandomItemId(), data)!!

        assertThat(node.getText("a")).isEqualTo("b")
    }

    @Test
    fun `json with data type`() {
        val data = """data:application/json;utf8, {"a": "b"} """
        val node = JsonPropertiesParser.parse(createRandomItemId(), data)!!

        assertThat(node.getText("a")).isEqualTo("b")
    }

    @Test
    fun `base64 json`() {
        val data = """data:application/json;base64,IHsiYSI6ICJiIn0g"""
        val node = JsonPropertiesParser.parse(createRandomItemId(), data)!!

        assertThat(node.getText("a")).isEqualTo("b")
    }
}