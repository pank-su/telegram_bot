package model

import kotlinx.serialization.Serializable


@Serializable
data class Profile(
    val telegram_id: String,
    val guap_id: String?,
    val guap_token: String?,
    val group_selected: Int?,
    val isAdmin: Boolean
)
