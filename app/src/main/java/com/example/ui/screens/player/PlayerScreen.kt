package com.example.ui.screens.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.MediaType
import com.example.data.model.StreamParams
import com.example.data.model.StreamProvider
import org.json.JSONObject

private const val ANTI_AD_JS = """
  (function () {
    try {
      window.open = function () { return null; };
      window.alert = function () {};
      window.confirm = function () { return false; };
      window.prompt = function () { return null; };
      if (!window.ReactNativeWebView) {
        window.ReactNativeWebView = {
          postMessage: function (msg) {
            try { if (window.AndroidBridge) window.AndroidBridge.postMessage(String(msg)); } catch (_) {}
          }
        };
      }
      if (!window.__sxMsgBound) {
        window.__sxMsgBound = true;
        window.addEventListener('message', function (ev) {
          try {
            var payload = typeof ev.data === 'string' ? ev.data : JSON.stringify(ev.data);
            if (payload && window.AndroidBridge) {
              window.AndroidBridge.postMessage(payload);
            }
          } catch (_) {}
        });
      }
      try {
        if (screen.orientation && screen.orientation.lock) {
          screen.orientation.lock = function () { return Promise.resolve(); };
        }
      } catch (_) {}
      var host = location.hostname;
      document.addEventListener('click', function (e) {
        var a = e.target && e.target.closest ? e.target.closest('a[target="_blank"]') : null;
        if (!a) return;
        try {
          var h = new URL(a.href, location.href).hostname;
          if (h !== host && !h.endsWith('.' + host)) {
            e.preventDefault(); e.stopPropagation();
          }
        } catch (_) { e.preventDefault(); e.stopPropagation(); }
      }, true);
    } catch (_) {}
  })();
"""

private const val CLEAN_UI_JS = """
  (function () {
    if (window.__sxCleanTimer) clearInterval(window.__sxCleanTimer);
    var tries = 0;
    window.__sxCleanTimer = setInterval(function () {
      tries++;
      var best = null, bestArea = 0;
      var pick = function (list) {
        for (var i = 0; i < list.length; i++) {
          var el = list[i];
          var a = el.clientWidth * el.clientHeight;
          if (a > bestArea) { bestArea = a; best = el; }
        }
      };
      pick(document.querySelectorAll('video'));
      pick(document.querySelectorAll('iframe'));
      pick(document.querySelectorAll('canvas'));
      if (!best || bestArea < 20000) {
        if (tries > 120) clearInterval(window.__sxCleanTimer);
        return;
      }
      var el = best;
      while (el.parentElement && el.parentElement !== document.body) {
        el = el.parentElement;
      }
      var top = el.parentElement === document.body ? el : document.body.firstElementChild;
      if (top && top.parentElement === document.body) {
        for (var k = 0; k < document.body.children.length; k++) {
          var c = document.body.children[k];
          if (c !== top) c.style.display = 'none';
        }
        top.style.cssText += ';position:fixed;top:0;left:0;width:100vw;height:100vh;z-index:2147483647;background:#000;overflow:hidden;';
      }
      best.style.cssText += ';width:100%!important;height:100%!important;object-fit:contain!important;';
      var a2 = best.parentElement;
      while (a2 && a2 !== document.body) {
        a2.style.setProperty('height', '100%', 'important');
        a2.style.setProperty('width', '100%', 'important');
        a2 = a2.parentElement;
      }
      document.body.style.overflow = 'hidden';
      document.documentElement.style.overflow = 'hidden';
    }, 1000);
  })();
"""

private class PlayerJsBridge(
    private val onProgressMessage: (positionSec: Int, durationSec: Int) -> Unit
) {
    @JavascriptInterface
    fun postMessage(raw: String) {
        try {
            val json = JSONObject(raw)
            if (json.optString("type") == "dominfo") return
            val nested = json.optJSONObject("data")
            val ct = when {
                nested != null && nested.has("currentTime") -> nested.optDouble("currentTime", -1.0)
                json.has("currentTime") -> json.optDouble("currentTime", -1.0)
                json.has("time") -> json.optDouble("time", -1.0)
                else -> -1.0
            }
            val dur = when {
                nested != null && nested.has("duration") -> nested.optDouble("duration", -1.0)
                json.has("duration") -> json.optDouble("duration", -1.0)
                else -> -1.0
            }
            if (ct >= 0.0 && dur > 0.0) {
                onProgressMessage(ct.toInt(), dur.toInt())
            }
        } catch (_: Exception) {
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(
    tmdbId: Int,
    mediaType: MediaType,
    season: Int,
    episode: Int,
    startPosition: Int,
    title: String,
    posterPath: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.Factory)
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }

    val providerIdx by viewModel.providerIdx.collectAsStateWithLifecycle()
    val seasonEpisodes by viewModel.seasonEpisodes.collectAsStateWithLifecycle()

    var activeEpisode by remember(episode) { mutableIntStateOf(episode.coerceAtLeast(1)) }
    var activePosition by remember(startPosition) { mutableIntStateOf(startPosition.coerceAtLeast(0)) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var showServers by remember { mutableStateOf(false) }
    var showEpisodes by remember { mutableStateOf(false) }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    LaunchedEffect(tmdbId, mediaType, season) {
        viewModel.loadSeasonEpisodes(tmdbId, mediaType, season.coerceAtLeast(1))
    }

    LaunchedEffect(tmdbId, mediaType, season, activeEpisode) {
        viewModel.markPlaybackStarted(
            tmdbId = tmdbId,
            mediaType = mediaType,
            title = title,
            posterPath = posterPath,
            season = season.coerceAtLeast(1),
            episode = activeEpisode
        )
    }

    val params = remember(tmdbId, mediaType, season, activeEpisode, activePosition) {
        StreamParams(
            type = mediaType,
            id = tmdbId,
            season = season.coerceAtLeast(1),
            episode = activeEpisode,
            position = activePosition
        )
    }

    val providers = StreamProvider.ALL
    val currentProvider = providers[providerIdx]
    val embedUrl = remember(currentProvider, params) { currentProvider.buildUrl(params) }
    val allowedHost = remember(embedUrl) { StreamProvider.hostOf(embedUrl) }

    DisposableEffect(activity, view) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        controller?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            customViewCallback?.onCustomViewHidden()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller?.show(WindowInsetsCompat.Type.systemBars())
            webViewRef?.destroy()
        }
    }

    BackHandler {
        if (customFullscreenView != null) {
            customViewCallback?.onCustomViewHidden()
            customFullscreenView = null
            customViewCallback = null
        } else {
            onBack()
        }
    }

    fun switchProvider(delta: Int) {
        loading = true
        loadError = false
        viewModel.selectProvider(providerIdx + delta)
    }

    fun selectEpisode(epNum: Int) {
        if (epNum < 1) return
        loading = true
        loadError = false
        activePosition = 0
        activeEpisode = epNum
        showEpisodes = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(AndroidColor.BLACK)
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        javaScriptCanOpenWindowsAutomatically = false
                        setSupportMultipleWindows(false)
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }
                    addJavascriptInterface(
                        PlayerJsBridge { pos, dur ->
                            viewModel.recordPlaybackProgress(
                                tmdbId = tmdbId,
                                mediaType = mediaType,
                                title = title,
                                posterPath = posterPath,
                                season = season.coerceAtLeast(1),
                                episode = activeEpisode,
                                positionSeconds = pos,
                                durationSeconds = dur
                            )
                        },
                        "AndroidBridge"
                    )
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            if (customFullscreenView != null) {
                                callback?.onCustomViewHidden()
                                return
                            }
                            customFullscreenView = view
                            customViewCallback = callback
                        }

                        override fun onHideCustomView() {
                            customFullscreenView = null
                            customViewCallback?.onCustomViewHidden()
                            customViewCallback = null
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val req = request ?: return false
                            if (!req.isForMainFrame) return false
                            val currentAllowed = view?.tag as? String ?: allowedHost
                            return !StreamProvider.isUrlAllowed(currentAllowed, req.url.toString())
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            loading = true
                            loadError = false
                            view?.evaluateJavascript(ANTI_AD_JS, null)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                            view?.evaluateJavascript(ANTI_AD_JS, null)
                            view?.evaluateJavascript(CLEAN_UI_JS, null)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                loading = false
                                loadError = true
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: WebResourceResponse?
                        ) {
                            if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 200) >= 400) {
                                loading = false
                                loadError = true
                            }
                        }
                    }
                    tag = allowedHost
                    loadUrl(embedUrl)
                    webViewRef = this
                }
            },
            update = { webView ->
                webView.tag = allowedHost
                val loadedKey = "${embedUrl}#$reloadNonce"
                if (webView.getTag(R.id.player_url_tag) != loadedKey) {
                    webView.setTag(R.id.player_url_tag, loadedKey)
                    loading = true
                    loadError = false
                    webView.loadUrl(embedUrl)
                }
            }
        )

        customFullscreenView?.let { fsView ->
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        (fsView.parent as? ViewGroup)?.removeView(fsView)
                        addView(
                            fsView,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                }
            )
        }

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = stringResource(R.string.player_loading, currentProvider.name),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (loadError && !loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = stringResource(R.string.player_failed, currentProvider.name),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                loading = true
                                loadError = false
                                reloadNonce++
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        ) {
                            Text(text = stringResource(R.string.player_retry))
                        }
                        Button(
                            onClick = { switchProvider(1) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(text = stringResource(R.string.player_next_server))
                        }
                    }
                }
            }
        }

        // Floating top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.scrim)
                    .testTag("player_back_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            val headerTitle = buildString {
                append(title.ifBlank { stringResource(R.string.player_now_playing) })
                if (mediaType == MediaType.TV) {
                    append(" · S${season.coerceAtLeast(1)}E$activeEpisode")
                }
            }
            Text(
                text = headerTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mediaType == MediaType.TV) {
                    val hasPrev = activeEpisode > 1
                    val maxEp = seasonEpisodes.maxOfOrNull { it.episodeNumber } ?: Int.MAX_VALUE
                    val hasNext = activeEpisode < maxEp

                    if (hasPrev) {
                        IconButton(
                            onClick = { selectEpisode(activeEpisode - 1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.scrim)
                                .testTag("player_prev_episode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = stringResource(R.string.player_prev_episode),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    if (hasNext) {
                        IconButton(
                            onClick = { selectEpisode(activeEpisode + 1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.scrim)
                                .testTag("player_next_episode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = stringResource(R.string.player_next_episode),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    if (seasonEpisodes.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                showEpisodes = !showEpisodes
                                if (showEpisodes) showServers = false
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (showEpisodes) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.scrim
                                )
                                .testTag("player_episodes_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FormatListNumbered,
                                contentDescription = stringResource(R.string.player_episodes),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        showServers = !showServers
                        if (showServers) showEpisodes = false
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (showServers) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.scrim
                        )
                        .testTag("player_servers_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = stringResource(R.string.cd_switch_server),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        if (showServers) {
            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(vertical = 10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                itemsIndexed(providers, key = { _, p -> p.id }) { idx, pr ->
                    val selected = idx == providerIdx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable {
                                if (idx != providerIdx) {
                                    loading = true
                                    loadError = false
                                    viewModel.selectProvider(idx)
                                }
                                showServers = false
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .testTag("provider_chip_${pr.id}")
                    ) {
                        Text(
                            text = pr.name,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showEpisodes && seasonEpisodes.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .background(Color.Black.copy(alpha = 0.84f))
                    .padding(vertical = 10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(seasonEpisodes, key = { it.episodeNumber }) { ep ->
                    val selected = ep.episodeNumber == activeEpisode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectEpisode(ep.episodeNumber) }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .testTag("player_ep_chip_${ep.episodeNumber}")
                    ) {
                        Text(
                            text = "E${ep.episodeNumber} · ${ep.name.ifBlank { "Episode ${ep.episodeNumber}" }}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
