package app.streamvibely

// Benchmark entry point (not part of the shipped app). Loads a folder of JPEG covers into a
// scrolling grid and auto-scrolls through it so memory can be sampled from outside.
//
// System properties:
//   bench.dir    folder with .jpg files (required)
//   bench.mode   "full" = decode at full size, "sub" = decode subsampled (default sub)
//   bench.sub    subsampling factor for "sub" (default 4: 1200px -> 300px)
//   bench.cache  max bitmaps kept in the LRU cache (default 40)
//   bench.passes number of scroll passes through the grid (default 3)

import app.streamvibely.theme.LegacyLime
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.streamvibely.theme.StreamvibelyTheme
import app.streamvibely.theme.Tokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import javax.imageio.ImageIO

private val dir = File(System.getProperty("bench.dir") ?: error("set -Dbench.dir"))
private val mode = System.getProperty("bench.mode") ?: "sub"
private val sub = (System.getProperty("bench.sub") ?: "4").toInt()
private val cacheMax = (System.getProperty("bench.cache") ?: "40").toInt()
private val passes = (System.getProperty("bench.passes") ?: "3").toInt()

private val count = (System.getProperty("bench.count") ?: "1000").toInt()
private val files = dir.listFiles { f -> f.extension == "jpg" }!!.sortedBy { it.name }.take(count)

private class Lru(private val max: Int) {
    private val map = object : LinkedHashMap<File, ImageBitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(e: MutableMap.MutableEntry<File, ImageBitmap>) = size > max
    }
    @Synchronized fun get(f: File) = map[f]
    @Synchronized fun put(f: File, b: ImageBitmap) { map[f] = b }
}

private val cache = Lru(cacheMax)

private fun decode(f: File): ImageBitmap {
    if (mode == "full") return ImageIO.read(f).toComposeImageBitmap()
    ImageIO.createImageInputStream(f).use { iis ->
        val reader = ImageIO.getImageReadersByFormatName("jpeg").next()
        reader.input = iis
        val p = reader.defaultReadParam
        p.setSourceSubsampling(sub, sub, 0, 0)
        val img = reader.read(0, p)
        reader.dispose()
        return img.toComposeImageBitmap()
    }
}

@Composable
private fun Cover(f: File) {
    val bmp by produceState<ImageBitmap?>(cache.get(f), f) {
        if (value == null) value = withContext(Dispatchers.IO) { decode(f).also { cache.put(f, it) } }
    }
    val shape = RoundedCornerShape(Tokens.radiusCard.dp)
    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(shape).background(LegacyLime.surfaceContainer)) {
        bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Artwork bench", state = rememberWindowState(width = 1280.dp, height = 800.dp)) {
        StreamvibelyTheme {
            val grid = rememberLazyGridState()
            LaunchedEffect(Unit) {
                delay(2000)
                repeat(passes) {
                    for (i in files.indices step 6) { grid.animateScrollToItem(i); delay(120) }
                    grid.animateScrollToItem(0)
                }
                println("BENCH_SCROLL_DONE mode=$mode cache=$cacheMax files=${files.size}")
            }
            LazyVerticalGrid(
                GridCells.Adaptive(160.dp), Modifier.fillMaxSize().background(LegacyLime.background),
                state = grid, contentPadding = PaddingValues(12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) { items(files) { Cover(it) } }
        }
    }
}
