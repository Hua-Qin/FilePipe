package dev.bikram.filepipe.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * Renders the app launcher icon, including adaptive icons (API 26+).
 * `painterResource(R.mipmap.ic_launcher)` crashes on adaptive icons, so we
 * use PackageManager to get the fully-rendered drawable and rasterize it.
 *
 * The [ImageBitmap] is cached in [remember]: calling [Bitmap.asImageBitmap] on every recomposition
 * allocates a new wrapper and makes [Image] redraw, which reads as flicker.
 */
@Composable
fun AppIconImage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageBitmap: ImageBitmap = remember(context.applicationContext.packageName) {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bmp ->
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
        }
        bitmap.asImageBitmap()
    }
    Image(
        bitmap = imageBitmap,
        contentDescription = null,
        modifier = modifier
    )
}
