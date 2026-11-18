package com.example.spoilershield

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class SpoilerAccessibilityService : AccessibilityService() {
    private var lastSwipeAt = 0L
    private var lastFingerprint = ""
    private var lastFingerprintAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        val packageName = event.packageName?.toString() ?: return
        val prefs = getSharedPreferences("shield", MODE_PRIVATE)

        val enabled = when (packageName) {
            FACEBOOK_PACKAGE -> prefs.getBoolean("facebook_enabled", true)
            YOUTUBE_PACKAGE -> prefs.getBoolean("youtube_enabled", true)
            else -> false
        }
        if (!enabled) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastSwipeAt < GLOBAL_COOLDOWN_MS) return

        val root = rootInActiveWindow ?: return
        val keywords = prefs.getString("keywords", MainActivity.DEFAULT_KEYWORDS)
            .orEmpty()
            .lineSequence()
            .map { normalize(it) }
            .filter { it.length >= 3 }
            .toList()
        if (keywords.isEmpty()) return

        val screen = screenRect()
        val match = findVisibleMatch(root, keywords, screen, 0) ?: return

        val fingerprint = "$packageName|${match.keyword}|${match.text.take(160)}"
        if (fingerprint == lastFingerprint && now - lastFingerprintAt < SAME_MATCH_COOLDOWN_MS) return

        val cardBounds = findLikelyCardBounds(match.node, screen)
        if (cardBounds.isEmpty) return

        // Ignore tiny text fragments and UI labels; a feed card should occupy a meaningful area.
        val minCardHeight = dp(170)
        if (cardBounds.height() < minCardHeight || cardBounds.width() < screen.width() * 0.62f) return

        lastFingerprint = fingerprint
        lastFingerprintAt = now
        lastSwipeAt = now
        swipeCardOut(cardBounds, screen)
    }

    private data class Match(
        val node: AccessibilityNodeInfo,
        val keyword: String,
        val text: String
    )

    private fun findVisibleMatch(
        node: AccessibilityNodeInfo,
        keywords: List<String>,
        screen: Rect,
        depth: Int
    ): Match? {
        if (depth > MAX_TREE_DEPTH || !node.isVisibleToUser) return null

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (Rect.intersects(screen, bounds)) {
            val candidates = listOfNotNull(
                node.text?.toString(),
                node.contentDescription?.toString()
            )
            for (raw in candidates) {
                val normalized = normalize(raw)
                if (normalized.isBlank()) continue
                val paddedText = " $normalized "
                val found = keywords.firstOrNull { keyword ->
                    paddedText.contains(" $keyword ")
                }
                if (found != null) return Match(node, found, normalized)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findVisibleMatch(child, keywords, screen, depth + 1)
            if (result != null) return result
        }
        return null
    }

    private fun findLikelyCardBounds(
        matchedNode: AccessibilityNodeInfo,
        screen: Rect
    ): Rect {
        var current: AccessibilityNodeInfo? = matchedNode
        var best = Rect()
        var bestArea = Long.MAX_VALUE
        var depth = 0

        while (current != null && depth < MAX_PARENT_DEPTH) {
            val candidate = Rect()
            current.getBoundsInScreen(candidate)

            val widthRatio = candidate.width().toFloat() / screen.width().coerceAtLeast(1)
            val height = candidate.height()
            val plausibleCard = Rect.intersects(screen, candidate) &&
                widthRatio >= 0.62f &&
                height >= dp(170) &&
                height <= screen.height() * 0.92f

            if (plausibleCard) {
                val area = candidate.width().toLong() * candidate.height().toLong()
                if (area < bestArea) {
                    best = Rect(candidate)
                    bestArea = area
                }
            }

            current = current.parent
            depth++
        }

        if (best.isEmpty) {
            matchedNode.getBoundsInScreen(best)
        }
        return best
    }

    private fun swipeCardOut(card: Rect, screen: Rect) {
        val x = card.centerX().toFloat().coerceIn(1f, (screen.right - 1).toFloat())
        val startY = min(card.bottom - dp(28), screen.bottom - dp(100))
            .coerceAtLeast((screen.top + dp(100)))
        val desiredDistance = (card.height() * 0.88f).toInt()
        val distance = desiredDistance.coerceIn(dp(240), dp(560))
        val endY = max(screen.top + dp(90), startY - distance).toFloat()

        if (startY - endY < dp(150)) return

        val path = Path().apply {
            moveTo(x, startY.toFloat())
            lineTo(x, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, SWIPE_DURATION_MS))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                // The feed may re-layout; the next content event will be scanned again.
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                // Do not immediately retry: avoid repeated accidental scrolling.
            }
        }, null)
    }

    private fun screenRect(): Rect {
        val metrics: DisplayMetrics = resources.displayMetrics
        return Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun normalize(value: String): String {
        return value.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    override fun onInterrupt() {
        // No persistent resources to release.
    }

    companion object {
        private const val FACEBOOK_PACKAGE = "com.facebook.katana"
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        private const val GLOBAL_COOLDOWN_MS = 1600L
        private const val SAME_MATCH_COOLDOWN_MS = 5000L
        private const val SWIPE_DURATION_MS = 320L
        private const val MAX_TREE_DEPTH = 38
        private const val MAX_PARENT_DEPTH = 10
    }
}
