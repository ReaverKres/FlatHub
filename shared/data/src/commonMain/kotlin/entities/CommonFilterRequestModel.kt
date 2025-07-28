package entities

import server_request.Currency

data class CommonFilterRequestModel(
    val priceFrom: Double? = null,
    val priceTo: Double? = null,
    val currency: Currency = Currency.USD
)
