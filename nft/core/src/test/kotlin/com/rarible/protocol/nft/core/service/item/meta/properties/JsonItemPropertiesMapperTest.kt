package com.rarible.protocol.nft.core.service.item.meta.properties

import com.rarible.protocol.nft.core.data.createRandomItemId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JsonItemPropertiesMapperTest {

    @Test
    fun `map - root data, ok`() {
        val json = """
            {
                "label" : "NAME",
                "description" : "DESCRIPTION",
                "image_url" : "ORIGINAL",
                "imageBig" : "BIG",
                "imagePreview" : "PREVIEW",
                "ledger_metadata" : {"ledger_stax_image" : "PORTRAIT" },
                "animation" : "VIDEO",                        
                "externalUri": "EXTERNAL"
           }
        """

        val node = JsonPropertiesParser.parse("", json)
        val properties = JsonItemPropertiesMapper.map(createRandomItemId(), node)

        assertThat(properties.name).isEqualTo("NAME")
        assertThat(properties.description).isEqualTo("DESCRIPTION")
        assertThat(properties.externalUri).isEqualTo("EXTERNAL")
        assertThat(properties.content.imageOriginal!!.url).isEqualTo("ORIGINAL")
        assertThat(properties.content.imagePreview!!.url).isEqualTo("PREVIEW")
        assertThat(properties.content.imageBig!!.url).isEqualTo("BIG")
        assertThat(properties.content.imagePortrait!!.url).isEqualTo("PORTRAIT")
        assertThat(properties.content.videoOriginal!!.url).isEqualTo("VIDEO")
    }

    @Test
    fun `map - minimum of data`() {
        val json = """
            {
                "name" : "NAME",
           }
        """

        val node = JsonPropertiesParser.parse("", json)
        val properties = JsonItemPropertiesMapper.map(createRandomItemId(), node)

        assertThat(properties.name).isEqualTo("NAME")
    }
}
