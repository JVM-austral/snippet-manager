package manager.service.asset

interface AssetServiceInterface {

    fun createAsset(
        container: String,
        key: String,
        data: String,
    ): String

    fun deleteAsset(
        container: String,
        key: String,
    ): String

    fun getAsset(
        container: String,
        key: String,
    ): String
}