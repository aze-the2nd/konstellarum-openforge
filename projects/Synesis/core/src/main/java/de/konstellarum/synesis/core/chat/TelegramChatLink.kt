package de.konstellarum.synesis.core.chat

data class TelegramChatLink(
    val appUri: String,
    val fallbackUrl: String,
) {
    companion object {
        private val validBotUsername = Regex("[A-Za-z0-9_]{5,32}")

        fun forBot(username: String): TelegramChatLink {
            val normalized = username.trim().removePrefix("@")
            require(validBotUsername.matches(normalized)) { "Invalid Telegram bot username" }
            return TelegramChatLink(
                appUri = "tg://resolve?domain=$normalized",
                fallbackUrl = "https://t.me/$normalized",
            )
        }
    }
}
