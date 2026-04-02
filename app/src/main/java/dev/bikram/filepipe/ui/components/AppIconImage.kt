package dev.bikram.filepipe.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * Renders the app launcher icon, including adaptive icons (API 26+).
 * `painterResource(R.mipmap.ic_launcher)` crashes on adaptive icons, so we
 * use PackageManager to get the fully-rendered drawable and rasterize it.
 */
@Composable
fun AppIconImage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(context) {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val size = 256
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bmp ->
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
        }
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = modifier
    )
}
