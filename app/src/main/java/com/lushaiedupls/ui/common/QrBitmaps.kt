package com.lushaiedupls.ui.common

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

fun encodeQrBitmap(content: String, sizePx: Int = 720): Bitmap? {
    if (content.isBlank()) return null
    return runCatching {
        val matrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            ),
        )
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bitmap
    }.getOrNull()
}

fun extractParentLinkToken(scanned: String): String {
    val raw = scanned.trim()
    if (raw.isEmpty()) return raw
    val tokenParam = Regex("[?&]token=([^&]+)").find(raw)?.groupValues?.getOrNull(1)
    if (!tokenParam.isNullOrBlank()) return java.net.URLDecoder.decode(tokenParam, "UTF-8")
    return raw
}
