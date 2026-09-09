package com.lushaiedupls.ui.student.ai

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.lushaiedupls.R
import com.lushaiedupls.data.mock.AiMenuContentItem
import com.lushaiedupls.data.remote.ResourcePlayback
import com.lushaiedupls.ui.common.AiMenuSectionHeaderSkeleton
import com.lushaiedupls.ui.common.AiResourceItemSkeleton
import com.lushaiedupls.ui.theme.BgWhite
import com.lushaiedupls.ui.theme.BorderGray
import com.lushaiedupls.ui.theme.BrandBlack
import com.lushaiedupls.ui.theme.BrandOrange
import com.lushaiedupls.ui.theme.TextSecondary

private val VideoCardShape = RoundedCornerShape(16.dp)
private val PlayerBlack = Color(0xFF111111)
private val AskChipShape = RoundedCornerShape(50)
private val HighlightPeach = Color(0xFFFFEFE6)

@Composable
fun ResourceVideosPage(
    resources: List<AiMenuContentItem>,
    isLoading: Boolean,
    onAskAboutResource: (AiMenuContentItem) -> Unit,
    highlightedResourceId: String? = null,
    scrollToResourceNonce: Int = 0,
    modifier: Modifier = Modifier,
) {
    var nowPlaying by remember { mutableStateOf<AiMenuContentItem?>(null) }
    val listState = rememberLazyListState()

    if (isLoading && resources.isEmpty()) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(BgWhite)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AiMenuSectionHeaderSkeleton(titleWidth = 92.dp, trailingWidth = 48.dp)
            }
            items(3) {
                AiResourceItemSkeleton()
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
        return
    }

    val playing = nowPlaying?.takeIf { resource -> resources.any { it.id == resource.id } }
    val queue = if (playing == null) resources else resources.filter { it.id != playing.id }

    LaunchedEffect(scrollToResourceNonce, highlightedResourceId, playing?.id, queue.size) {
        val targetId = highlightedResourceId ?: return@LaunchedEffect
        val queueIndex = queue.indexOfFirst { it.id == targetId }
        val index = when {
            playing?.id == targetId -> 0
            queueIndex < 0 -> return@LaunchedEffect
            playing != null -> 1 + (if (queue.isNotEmpty()) 1 else 0) + queueIndex
            else -> 1 + queueIndex
        }
        listState.animateScrollToItem(index)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(BgWhite),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (playing != null) {
            val playingHighlighted = playing.id == highlightedResourceId
            item(key = "player_${playing.id}") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = VideoCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (playingHighlighted) HighlightPeach else BgWhite,
                    ),
                    border = BorderStroke(
                        width = if (playingHighlighted) 2.dp else 1.dp,
                        color = if (playingHighlighted) BrandOrange else BorderGray.copy(alpha = 0.65f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .background(PlayerBlack),
                        ) {
                            ResourcePlayer(
                                videoUrl = playing.videoUrl.orEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f),
                            )
                            IconButton(
                                onClick = { nowPlaying = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.cd_close),
                                    tint = Color.White,
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                        ) {
                            Text(
                                text = playing.title,
                                color = BrandBlack,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ResourceAskFooter(
                                subtitle = playing.subtitle,
                                onAsk = { onAskAboutResource(playing) },
                            )
                        }
                    }
                }
            }
            if (queue.isNotEmpty()) {
                item(key = "up_next") {
                    Text(
                        text = stringResource(R.string.ai_resource_up_next),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandBlack,
                        fontFamily = FontFamily.SansSerif,
                    )
                }
            }
        } else {
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.ai_menu_resources),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BrandBlack,
                        fontFamily = FontFamily.SansSerif,
                    )
                    Text(
                        text = stringResource(R.string.ai_resource_count, resources.size),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontFamily = FontFamily.SansSerif,
                    )
                }
            }
        }

        items(queue, key = { it.id.ifBlank { it.title } }) { item ->
            ResourceVideoCard(
                item = item,
                highlighted = item.id == highlightedResourceId,
                onPlay = {
                    if (!item.videoUrl.isNullOrBlank()) {
                        nowPlaying = item
                    }
                },
                onAsk = { onAskAboutResource(item) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun ResourceVideoCard(
    item: AiMenuContentItem,
    onPlay: () -> Unit,
    onAsk: () -> Unit,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val playable = !item.videoUrl.isNullOrBlank()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = VideoCardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) HighlightPeach else BgWhite,
        ),
        border = BorderStroke(
            width = if (highlighted) 2.dp else 1.dp,
            color = if (highlighted) BrandOrange else BorderGray.copy(alpha = 0.65f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(PlayerBlack)
                    .clickable(enabled = playable, onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.22f)),
                )
                if (playable) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.ai_resource_play),
                            tint = Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            ) {
                Text(
                    text = item.title,
                    color = BrandBlack,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(12.dp))
                ResourceAskFooter(
                    subtitle = item.subtitle,
                    onAsk = onAsk,
                )
            }
        }
    }
}

@Composable
private fun ResourceAskFooter(
    subtitle: String?,
    onAsk: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = subtitle.orEmpty(),
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            color = TextSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        AskAiChip(onClick = onAsk)
    }
}

@Composable
private fun AskAiChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(AskChipShape)
            .background(BrandBlack)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = BrandOrange,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = "Ask",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            fontFamily = FontFamily.SansSerif,
        )
    }
}

@Composable
private fun ResourcePlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
) {
    val youtubeId = remember(videoUrl) { ResourcePlayback.youtubeId(videoUrl) }
    key(videoUrl) {
        if (youtubeId != null) {
            YouTubePlayer(videoId = youtubeId, modifier = modifier)
        } else {
            FileVideoPlayer(videoUrl = videoUrl, modifier = modifier)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubePlayer(
    videoId: String,
    modifier: Modifier = Modifier,
) {
    val embedUrl = remember(videoId) { ResourcePlayback.youtubeEmbedUrl(videoId) }
    AndroidView(
        modifier = modifier.background(PlayerBlack),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                setBackgroundColor(android.graphics.Color.BLACK)
                loadUrl(embedUrl)
            }
        },
        update = { webView ->
            if (webView.url != embedUrl) {
                webView.loadUrl(embedUrl)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        },
    )
}

@Composable
private fun FileVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.background(PlayerBlack),
        factory = { context ->
            FrameLayout(context).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                val videoView = VideoView(context)
                val controller = MediaController(context)
                controller.setAnchorView(this)
                videoView.setMediaController(controller)
                videoView.setVideoURI(Uri.parse(videoUrl))
                addView(
                    videoView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                videoView.setOnPreparedListener { it.isLooping = false; videoView.start() }
            }
        },
        onRelease = { frame ->
            val video = frame.getChildAt(0) as? VideoView
            video?.stopPlayback()
        },
    )
}
