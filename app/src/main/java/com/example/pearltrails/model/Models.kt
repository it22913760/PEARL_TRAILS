package com.example.pearltrails.model

data class Destination(
    val title: String,
    val subtitle: String,
    val description: String? = null,
    val rating: Double,
    val imageRes: Int,
    val category: Category? = null
)

enum class Category {
    CULTURE, NATURE, BEACHES, HERITAGE, WILDLIFE, SCENIC, EXPLORE
}
