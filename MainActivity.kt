package com.adm.player

import android.Manifest
import android.content.Context
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------- রং ----------
private val Ink = Color(0xFF111111)
private val Sub = Color(0xFF6B7078)

// ---------- ডেটা ----------
data class Video(
    val id: Long,
    val uri: Uri,
    val title: String,
    val durationMs: Long,
    val size: Long,
    val folder: String
)

fun queryVideos(ctx: Context): List<Video> {
    val out = mutableListOf<Video>()
    val proj = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME
    )
    ctx.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        proj, null, null,
        MediaStore.Video.Media.DATE_ADDED + " DESC"
    )?.use { c ->
        val iId = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val iName = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val iDur = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val iSize = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val iFolder = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        while (c.moveToNext()) {
            val id = c.getLong(iId)
            out.add(
                Video(
                    id = id,
                    uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                    title = c.getString(iName) ?: "Video",
                    durationMs = c.getLong(iDur),
                    size = c.getLong(iSize),
                    folder = c.getString(iFolder) ?: "Other"
                )
            )
        }
    }
    return out
}

fun fmtTime(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

// ---------- গ্লাস স্টাইল ----------
fun Modifier.glass(r: Dp = 24.dp): Modifier {
    val shape = RoundedCornerShape(r)
    return this
        .shadow(14.dp, shape, ambientColor = Color(0x22000000), spotColor = Color(0x33000000))
        .background(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEDEFF3))), shape)
        .border(1.dp, Color.White, shape)
}

// ---------- Activity ----------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) { App() }
        }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val perm = if (Build.VERSION.SDK_INT >= 33)
        Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE

    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(perm) }

    var videos by remember { mutableStateOf(listOf<Video>()) }
    LaunchedEffect(granted) {
        if (granted) videos = withContext(Dispatchers.IO) { queryVideos(ctx) }
    }

    var playing by remember { mutableStateOf<Video?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color.White, Color(0xFFE3E6EB))))
    ) {
        val v = playing
        if (v != null) {
            Player(v) { playing = null }
        } else {
            Home(videos, granted, onAsk = { launcher.launch(perm) }, onOpen = { playing = it })
        }
    }
}

// ---------- হোম (ভিডিও লিস্ট) ----------
@Composable
fun Home(videos: List<Video>, granted: Boolean, onAsk: () -> Unit, onOpen: (Video) -> Unit) {
    val ctx = LocalContext.current
    val loader = remember { ImageLoader.Builder(ctx).components { add(VideoFrameDecoder.Factory()) }.build() }
    var folder by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    val folders = remember(videos) { videos.map { it.folder }.distinct().sorted() }
    val shown = remember(videos, folder, query) {
        videos.filter { (folder == null || it.folder == folder) && it.title.contains(query, true) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Videos", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("${videos.size} videos", fontSize = 14.sp, color = Sub)
        Spacer(Modifier.height(16.dp))

        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search", color = Sub) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Color(0xCCFFFFFF),
                unfocusedContainerColor = Color(0xCCFFFFFF)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Chip("All", folder == null) { folder = null } }
            items(folders) { f -> Chip(f, folder == f) { folder = f } }
        }
        Spacer(Modifier.height(16.dp))

        if (!granted) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .glass(24.dp)
                        .clickable { onAsk() }
                        .padding(horizontal = 28.dp, vertical = 18.dp)
                ) { Text("Allow video access", fontWeight = FontWeight.SemiBold, color = Ink) }
            }
        } else if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No videos found", color = Sub, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.navigationBarsPadding()
            ) {
                items(shown, key = { it.id }) { v ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .glass(24.dp)
                            .clickable { onOpen(v) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = v.uri,
                            imageLoader = loader,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 104.dp, height = 68.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFD9DCE2))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                v.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                v.folder + "  •  " + fmtTime(v.durationMs) + "  •  " +
                                        Formatter.formatShortFileSize(ctx, v.size),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp, color = Sub
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) Ink else Color(0xCCFFFFFF), shape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else Ink, maxLines = 1
        )
    }
}

// ---------- প্লেয়ার ----------
@Composable
fun Player(v: Video, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val exo = remember(v) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(v.uri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(exo) { onDispose { exo.release() } }
    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { c ->
                PlayerView(c).apply {
                    player = exo
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .statusBarsPadding()
                .padding(12.dp)
                .size(44.dp)
                .glass(22.dp)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) { Text("←", fontSize = 20.sp, color = Ink) }
    }
}
