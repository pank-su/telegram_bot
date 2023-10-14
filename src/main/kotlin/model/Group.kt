package model

import kotlinx.serialization.Serializable


@Serializable
data class Group(val group_id: Int, val group_name: String)
