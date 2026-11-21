package com.dosetrack.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import com.dosetrack.R
import com.dosetrack.data.model.SitePoint

/** Fixed, bundled anatomical injection-site guide points; not user data. */
object InjectionGuidePoints {
    val points = listOf(
        SitePoint(0.4f, 0.367f), // left of belly button
        SitePoint(0.6f, 0.367f), // right of belly button
        SitePoint(0.25f, 0.283f), // left upper arm
        SitePoint(0.75f, 0.283f), // right upper arm
        SitePoint(0.367f, 0.667f), // left outer thigh
        SitePoint(0.633f, 0.667f), // right outer thigh
    )
}

@Composable
fun BodyDiagram(
    currentSite: SitePoint?,
    modifier: Modifier = Modifier,
    historicalSites: List<SitePoint> = emptyList(),
    showGuide: Boolean = false,
    guidePoints: List<SitePoint> = InjectionGuidePoints.points,
    isInteractive: Boolean = true,
    onSiteSelected: (SitePoint) -> Unit = {},
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.5f)
            .onSizeChanged { boxSize = it }
            .then(
                if (isInteractive) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures { offset ->
                            if (boxSize.width > 0 && boxSize.height > 0) {
                                val x = (offset.x / boxSize.width).coerceIn(0f, 1f)
                                val y = (offset.y / boxSize.height).coerceIn(0f, 1f)
                                onSiteSelected(SitePoint(x, y))
                            }
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Image(
            painter = painterResource(R.drawable.body_diagram_front),
            contentDescription = "Body diagram",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.5f),
        )
        MarkerOverlay(
            currentSite = currentSite,
            historicalSites = historicalSites,
            guidePoints = guidePoints,
            showGuide = showGuide,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.5f),
        )
    }
}
