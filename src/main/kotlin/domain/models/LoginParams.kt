package domain.models

data class LoginParams(
    var phoneNumber: Long,
    var apiId: Int,
    var apiHash: String
)
