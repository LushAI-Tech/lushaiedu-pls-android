package com.lushaiedupls.data.remote.device

internal object DeviceProfile {
    const val DEVICE_NAME_MAX = 150

    fun deviceName(manufacturer: String?, model: String?): String {
        val modelName = model?.trim().orEmpty()
        val brand = manufacturer?.trim().orEmpty()
        val combined = when {
            modelName.isBlank() -> brand
            brand.isBlank() || modelName.contains(brand, ignoreCase = true) -> modelName
            else -> "$brand $modelName"
        }.replace(Regex("\\s+"), " ").trim()
        return combined.take(DEVICE_NAME_MAX).ifBlank { "Android" }
    }

    fun userAgent(
        appVersion: String,
        androidRelease: String?,
        model: String?,
        sdkInt: Int,
    ): String {
        val release = androidRelease?.trim().orEmpty().ifBlank { sdkInt.toString() }
        val device = model?.trim().orEmpty().ifBlank { "Android" }
        return "LushAIEdu_PLS/$appVersion (Android $release; $device; sdk $sdkInt)"
    }
}
