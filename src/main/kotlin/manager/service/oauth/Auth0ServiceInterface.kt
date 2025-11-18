package manager.service.oauth

interface Auth0ServiceInterface {
    fun getM2MToken(): String

    fun getUserName(id: String): String
}
