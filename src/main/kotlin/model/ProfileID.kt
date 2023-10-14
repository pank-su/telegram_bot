package model

import kotlinx.serialization.Serializable


@Serializable
data class ProfileID(val telegram_id: String)
