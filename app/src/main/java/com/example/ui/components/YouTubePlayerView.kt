package com.example.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class YouTubePlayerController {
    var seekTo: ((seconds: Int) -> Unit)? = null
    var seekRelative: ((deltaSeconds: Int) -> Unit)? = null
    var setPlaybackSpeed: ((speed: Float) -> Unit)? = null
    var setPlaybackQuality: ((quality: String) -> Unit)? = null
    var play: (() -> Unit)? = null
    var pause: (() -> Unit)? = null
    var togglePlayPause: (() -> Unit)? = null
    var setLoopRange: ((startSec: Int, endSec: Int) -> Unit)? = null
    var clearLoop: (() -> Unit)? = null
}

class YouTubeBridge(
    private val onProgress: (current: Int, total: Int) -> Unit,
    private val onStateChanged: (isPlaying: Boolean) -> Unit,
    private val onError: (errorCode: Int) -> Unit
) {
    @JavascriptInterface
    fun onTimeUpdate(currentTime: Float, duration: Float) {
        onProgress(currentTime.toInt(), duration.toInt())
    }

    @JavascriptInterface
    fun onPlayerStateChange(state: Int) {
        // 1 = PLAYING, 2 = PAUSED, 0 = ENDED, 3 = BUFFERING
        onStateChanged(state == 1)
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
    playbackQuality: String = "auto",
    controller: YouTubePlayerController? = null,
    onProgressUpdate: (current: Int, total: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isLoading by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var reloadTrigger by remember { mutableIntStateOf(0) }

    var currentSec by remember { mutableIntStateOf(startSeconds) }
    var totalSec by remember { mutableIntStateOf(0) }

    // HTML5 Fullscreen support via WebChromeClient custom view
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Wire controller commands to WebView JS
    LaunchedEffect(webViewRef) {
        controller?.apply {
            seekTo = { sec ->
                currentSec = sec
                webViewRef?.evaluateJavascript(
                    "if (window.player && player.seekTo) { player.seekTo($sec, true); player.playVideo(); }",
                    null
                )
            }
            seekRelative = { delta ->
                val target = (currentSec + delta).coerceIn(0, if (totalSec > 0) totalSec else Int.MAX_VALUE)
                currentSec = target
                webViewRef?.evaluateJavascript(
                    "if (window.player && typeof player.getCurrentTime === 'function') { var c = (player.getCurrentTime() || 0) + ($delta); player.seekTo(Math.max(0, c), true); }",
                    null
                )
            }
            setPlaybackSpeed = { speed ->
                webViewRef?.evaluateJavascript(
                    "if (window.player && player.setPlaybackRate) { player.setPlaybackRate($speed); }",
                    null
                )
            }
            setPlaybackQuality = { quality ->
                webViewRef?.evaluateJavascript(
                    "if (window.player && player.setPlaybackQuality) { player.setPlaybackQuality('$quality'); }",
                    null
                )
            }
            play = {
                webViewRef?.evaluateJavascript(
                    "if (window.player && player.playVideo) { player.playVideo(); }",
                    null
                )
            }
            pause = {
                webViewRef?.evaluateJavascript(
                    "if (window.player && player.pauseVideo) { player.pauseVideo(); }",
                    null
                )
            }
            togglePlayPause = {
                if (isPlaying) {
                    webViewRef?.evaluateJavascript("if(window.player && player.pauseVideo){ player.pauseVideo(); }", null)
                } else {
                    webViewRef?.evaluateJavascript("if(window.player && player.playVideo){ player.playVideo(); }", null)
                }
            }
            setLoopRange = { start, end ->
                webViewRef?.evaluateJavascript(
                    "window.loopStart = $start; window.loopEnd = $end; if (window.player && player.seekTo) { player.seekTo($start, true); player.playVideo(); }",
                    null
                )
            }
            clearLoop = {
                webViewRef?.evaluateJavascript(
                    "window.loopStart = null; window.loopEnd = null;",
                    null
                )
            }
        }
    }

    // Handle back press while in video custom fullscreen mode
    BackHandler(enabled = customView != null) {
        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    // Update speed dynamically when state changes
    LaunchedEffect(playbackSpeed) {
        webViewRef?.evaluateJavascript(
            "if (window.player && player.setPlaybackRate) { player.setPlaybackRate($playbackSpeed); }",
            null
        )
    }

    // Update quality dynamically when state changes
    LaunchedEffect(playbackQuality) {
        webViewRef?.evaluateJavascript(
            "if (window.player && player.setPlaybackQuality) { player.setPlaybackQuality('$playbackQuality'); }",
            null
        )
    }

    DisposableEffect(videoId) {
        onDispose {
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
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
                #player-wrapper { position: relative; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background-color: #000000; }
                #player { width: 100%; height: 100%; border: none; }
                .ytp-pause-overlay, .ytp-scroll-min { display: none !important; }
            </style>
        </head>
        <body>
            <div id="player-wrapper">
                <div id="player"></div>
            </div>
            <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                var player;
                var progressInterval;
                window.loopStart = null;
                window.loopEnd = null;

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
                            'enablejsapi': 1,
                            'origin': 'https://www.youtube-nocookie.com',
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
                        if ('$playbackQuality' !== 'auto') {
                            event.target.setPlaybackQuality('$playbackQuality');
                        }
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

                                // A/B Looping
                                if (window.loopStart !== null && window.loopEnd !== null && window.loopEnd > window.loopStart) {
                                    if (curr >= window.loopEnd) {
                                        player.seekTo(window.loopStart, true);
                                        return;
                                    }
                                }

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
            .background(Color.Black)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (customView != null) {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(0xFF000000.toInt())
                        val cv = customView
                        if (cv?.parent != null) {
                            (cv.parent as? ViewGroup)?.removeView(cv)
                        }
                        if (cv != null) {
                            addView(
                                cv,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    }
                },
                update = { container ->
                    val cv = customView
                    if (cv != null && cv.parent != container) {
                        (cv.parent as? ViewGroup)?.removeView(cv)
                        container.removeAllViews()
                        container.addView(
                            cv,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
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

                            // High-compatibility Chrome Mobile User-Agent
                            val originalUa = userAgentString ?: ""
                            val cleanedUa = originalUa
                                .replace("; wv", "")
                                .replace("Version/4.0 ", "")
                            userAgentString = if (cleanedUa.isNotBlank()) cleanedUa else "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                        }
                        addJavascriptInterface(
                            YouTubeBridge(
                                onProgress = { cur, tot ->
                                    currentSec = cur
                                    totalSec = tot
                                    onProgressUpdate(cur, tot)
                                },
                                onStateChanged = { playing ->
                                    isPlaying = playing
                                },
                                onError = { code ->
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

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString().orEmpty()
                                if (url.startsWith("intent://") || url.startsWith("vnd.youtube:")) {
                                    return true
                                }
                                return false
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                customView = view
                                customViewCallback = callback
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            }

                            override fun onHideCustomView() {
                                customView = null
                                customViewCallback?.onCustomViewHidden()
                                customViewCallback = null
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        }
                        loadDataWithBaseURL("https://www.youtube-nocookie.com", htmlContent, "text/html", "UTF-8", null)
                        webViewRef = this
                    }
                },
                update = { wv ->
                    webViewRef = wv
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading Spinner
        if (isLoading && customView == null) {
            CircularProgressIndicator(
                color = FocusIndigo,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
        }

        // Fallback UI if creator restricted embedding
        playerError?.let {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF5080C14))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0x22F59E0B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = FocusAmber,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Embedding Restricted by Creator",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "This specific video creator disabled third-party embed playback. You can still stream it while taking notes and keeping your study timer active.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
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
                                    } catch (_: Exception) { }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FocusIndigo),
                            shape = RoundedCornerShape(10.dp)
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
                                    "https://www.youtube-nocookie.com",
                                    htmlContent,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = TextSecondary
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Retry", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary))
                        }
                    }
                }
            }
        }
    }
}
