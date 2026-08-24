package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class JavaScriptBridge(
    private val onProgress: (current: Int, total: Int) -> Unit,
    private val onStateChanged: (state: Int) -> Unit,
    private val onError: (errorCode: Int) -> Unit
) {
    @JavascriptInterface
    fun onTimeUpdate(currentTime: Float, duration: Float) {
        onProgress(currentTime.toInt(), duration.toInt())
    }

    @JavascriptInterface
    fun onPlayerStateChange(state: Int) {
        onStateChanged(state)
    }

    @JavascriptInterface
    fun onPlayerError(code: Int) {
        onError(code)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerView(
    videoId: String,
    startSeconds: Int = 0,
    playbackSpeed: Float = 1.0f,
    onProgressUpdate: (current: Int, total: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var reloadTrigger by remember { mutableIntStateOf(0) }

    // Update playback speed dynamically
    LaunchedEffect(playbackSpeed) {
        webViewRef?.evaluateJavascript(
            "if (window.player && player.setPlaybackRate) { player.setPlaybackRate($playbackSpeed); }",
            null
        )
    }

    DisposableEffect(videoId) {
        onDispose {
            webViewRef?.let { wv ->
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.destroy()
            }
        }
    }

    val htmlContent = remember(videoId, startSeconds, reloadTrigger) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body { width: 100%; height: 100%; background-color: #000000; overflow: hidden; }
                #player { width: 100%; height: 100%; border: none; }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                var player;
                var progressInterval;

                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        width: '100%',
                        height: '100%',
                        videoId: '$videoId',
                        playerVars: {
                            'autoplay': 1,
                            'playsinline': 1,
                            'controls': 1,
                            'rel': 0,
                            'modestbranding': 1,
                            'iv_load_policy': 3,
                            'fs': 1,
                            'start': $startSeconds
                        },
                        events: {
                            'onReady': onPlayerReady,
                            'onStateChange': onPlayerStateChange,
                            'onError': onPlayerError
                        }
                    });
                }

                function onPlayerReady(event) {
                    try {
                        event.target.setPlaybackRate($playbackSpeed);
                        event.target.playVideo();
                    } catch(e) {}
                    startProgressTracker();
                }

                function onPlayerStateChange(event) {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onPlayerStateChange(event.data);
                    }
                    if (event.data === 1) { // PLAYING
                        startProgressTracker();
                    } else if (event.data === 2 || event.data === 0) { // PAUSED or ENDED
                        stopProgressTracker();
                    }
                }

                function onPlayerError(event) {
                    // Only dispatch error for owner restriction codes (101 / 150)
                    if (event && (event.data === 101 || event.data === 150)) {
                        if (window.AndroidBridge) {
                            window.AndroidBridge.onPlayerError(event.data);
                        }
                    }
                }

                function startProgressTracker() {
                    stopProgressTracker();
                    progressInterval = setInterval(function() {
                        try {
                            if (player && typeof player.getCurrentTime === 'function' && typeof player.getDuration === 'function') {
                                var curr = player.getCurrentTime() || 0;
                                var dur = player.getDuration() || 0;
                                if (window.AndroidBridge && dur > 0) {
                                    window.AndroidBridge.onTimeUpdate(curr, dur);
                                }
                            }
                        } catch(e) {}
                    }, 1000);
                }

                function stopProgressTracker() {
                    if (progressInterval) {
                        clearInterval(progressInterval);
                        progressInterval = null;
                    }
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = modifier
            .background(ObsidianBg)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(0xFF000000.toInt())
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        allowFileAccess = true
                        allowContentAccess = true
                        
                        // Fix YouTube Error 152-4 by replacing WebView User-Agent signature with Chrome Mobile
                        val originalUa = userAgentString ?: ""
                        val cleanedUa = originalUa
                            .replace("; wv", "")
                            .replace("Version/4.0 ", "")
                        userAgentString = if (cleanedUa.isNotBlank()) cleanedUa else "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                    }
                    addJavascriptInterface(
                        JavaScriptBridge(
                            onProgress = onProgressUpdate,
                            onStateChanged = { /* handled */ },
                            onError = { code ->
                                // Error 101/150/152 indicates embedding restrictions by channel or origin
                                playerError = "Playback restricted by video creator (Code $code)"
                            }
                        ),
                        "AndroidBridge"
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            playerError = null
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            isLoading = false
                        }
                    }
                    webChromeClient = WebChromeClient()
                    loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            update = { wv ->
                webViewRef = wv
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            CircularProgressIndicator(
                color = FocusIndigo
            )
        }

        // Dedicated Fallback UI if creator restricted embedding or YouTube blocked iframe
        playerError?.let { err ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE080C14))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = FocusAmber,
                        modifier = Modifier.size(42.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Embedding Restricted by Video Owner",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "The creator of this video does not allow playback in embedded players outside YouTube. You can still open it directly while keeping your study session active.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val url = "https://www.youtube.com/watch?v=$videoId"
                                val pm = context.packageManager
                                val browserTest = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                                    addCategory(Intent.CATEGORY_BROWSABLE)
                                }
                                val resolveList = try {
                                    pm.queryIntentActivities(browserTest, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                                } catch (e: Exception) {
                                    emptyList()
                                }
                                val browserPkg = resolveList.firstOrNull { info ->
                                    val pkg = info.activityInfo.packageName.lowercase()
                                    (pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("firefox") ||
                                     pkg.contains("opera") || pkg.contains("brave") || pkg.contains("edge")) && !pkg.contains("youtube")
                                }?.activityInfo?.packageName ?: resolveList.firstOrNull { info ->
                                    !info.activityInfo.packageName.lowercase().contains("youtube")
                                }?.activityInfo?.packageName

                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    addCategory(Intent.CATEGORY_BROWSABLE)
                                    if (browserPkg != null) {
                                        setPackage(browserPkg)
                                    }
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        })
                                    } catch (e2: Exception) { }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FocusIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("open_in_browser_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "Open in Web Browser",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                playerError = null
                                isLoading = true
                                reloadTrigger++
                                webViewRef?.loadDataWithBaseURL(
                                    "https://www.youtube.com",
                                    htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    "https://www.youtube.com"
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("retry_player_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = TextSecondary
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "Retry",
                                style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                            )
                        }
                    }
                }
            }
        }
    }
}
