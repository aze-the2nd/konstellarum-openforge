package de.konstellarum.synesis.core.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TelegramChatLinkTest {

    @Test
    fun `bot username can be passed with or without at sign`() {
        assertEquals(
            TelegramChatLink(
                appUri = "tg://resolve?domain=tommy_watson_bot",
                fallbackUrl = "https://t.me/tommy_watson_bot",
            ),
            TelegramChatLink.forBot("@tommy_watson_bot"),
        )
    }

    @Test
    fun `bot username is trimmed`() {
        assertEquals(
            "tg://resolve?domain=tommy_watson_bot",
            TelegramChatLink.forBot("  tommy_watson_bot  ").appUri,
        )
    }

    @Test
    fun `invalid bot username is rejected before building a uri`() {
        assertFailsWith<IllegalArgumentException> {
            TelegramChatLink.forBot("tommy watson bot")
        }
    }
}
