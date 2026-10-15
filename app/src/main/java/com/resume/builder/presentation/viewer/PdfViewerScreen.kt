package com.resume.builder.presentation.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resume.builder.presentation.components.FileActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File

private const val MAX_ZOOM = 4f
private const val MAX_RENDER_WIDTH = 2048

@Composable
fun PdfViewerScreen(path: String, title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val document by produceState<ViewerState>(ViewerState.Loading, path) {
        value = withContext(Dispatchers.IO) {
            PdfPageSource.open(File(path))?.let { ViewerState.Ready(it) } ?: ViewerState.Error
        }
    }
    val ready = document as? ViewerState.Ready
    DisposableEffect(ready) { onDispose { ready?.source?.close() } }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { FileActions.share(context, path, "application/pdf", title) }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            when (val state = document) {
                ViewerState.Loading -> CircularProgressIndicator()
                ViewerState.Error -> ViewerError()
                is ViewerState.Ready -> PdfPages(state.source)
            }
        }
    }
}

@Composable
private fun PdfPages(source: PdfPageSource) {
    val listState = rememberLazyListState()
    val horizontal = rememberScrollState()
    var scale by remember { mutableFloatStateOf(1f) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            // Pinch zoom is handled before the list sees the touches; one-finger drags still scroll.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } >= 2) {
                            val zoom = event.calculateZoom()
                            val newScale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                            val factor = newScale / scale
                            if (factor != 1f) {
                                val centroid = event.calculateCentroid()
                                scale = newScale
                                horizontal.dispatchRawDelta((horizontal.value + centroid.x) * (factor - 1f))
                                listState.dispatchRawDelta((listState.firstVisibleItemScrollOffset + centroid.y) * (factor - 1f))
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { scale = if (scale > 1f) 1f else 2f })
            },
    ) {
        val contentWidth = maxWidth * scale
        val pageWidthPx = with(LocalDensity.current) { (contentWidth - 32.dp).roundToPx() }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxHeight().horizontalScroll(horizontal).width(contentWidth),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(source.pageCount) { index ->
                PdfPage(source, index, pageWidthPx)
            }
        }

        if (source.pageCount > 1) {
            val current by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp),
            ) {
                Text(
                    "$current / ${source.pageCount}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PdfPage(source: PdfPageSource, index: Int, targetWidthPx: Int) {
    var bitmap by remember(index) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(index, targetWidthPx) {
        // Wait for pinch gestures to settle, then re-render sharp at the new size.
        if (bitmap != null) delay(180)
        withContext(Dispatchers.IO) { source.render(index, targetWidthPx) }?.let { bitmap = it }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(source.pageAspect(index))
            .shadow(3.dp, RoundedCornerShape(4.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        val page = bitmap
        if (page != null) {
            Image(page.asImageBitmap(), contentDescription = "Page ${index + 1}", contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        } else {
            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun ViewerError() {
    Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.PictureAsPdf, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text("Can't open this PDF", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "The file may have been moved, deleted or is damaged.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private sealed interface ViewerState {
    data object Loading : ViewerState
    data object Error : ViewerState
    data class Ready(val source: PdfPageSource) : ViewerState
}

/** Thread-safe wrapper around [PdfRenderer]; pages render one at a time. */
private class PdfPageSource private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) : Closeable {
    private val lock = Any()
    private var closed = false

    val pageCount: Int = renderer.pageCount
    private val aspects: FloatArray = FloatArray(pageCount) { i ->
        val page = renderer.openPage(i)
        try { page.width.toFloat() / page.height } finally { page.close() }
    }

    fun pageAspect(index: Int): Float = aspects[index]

    fun render(index: Int, widthPx: Int): Bitmap? = synchronized(lock) {
        if (closed) return null
        val page = renderer.openPage(index)
        try {
            val width = widthPx.coerceIn(1, MAX_RENDER_WIDTH)
            val height = (width / aspects[index]).toInt().coerceAtLeast(1)
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.WHITE)
                page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        } finally {
            page.close()
        }
    }

    override fun close() = synchronized(lock) {
        if (!closed) {
            closed = true
            renderer.close()
            descriptor.close()
        }
    }

    companion object {
        fun open(file: File): PdfPageSource? {
            if (!file.exists()) return null
            val descriptor = runCatching { ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) }.getOrNull() ?: return null
            return runCatching { PdfPageSource(descriptor, PdfRenderer(descriptor)) }
                .onFailure { descriptor.close() }
                .getOrNull()
        }
    }
}
