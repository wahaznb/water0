/*
 * Liquid-glass backdrop technique ported from
 * https://github.com/Mortd3kay/liquid-glass-android
 * (Copyright 2024 MRTDK, Apache License 2.0).
 *
 * Modifications for Water0: package rename; Kotlin 1.9-compatible source
 * (no language changes needed); removed the debug println; mapped the
 * lens parameters onto GlassConfig (apply-on-restart); predicate kept:
 * API 33+ AGSL shader path, gradient fallback below.
 *
 * Architecture (unchanged): LiquidGlassContainer draws `content` through
 * a single RuntimeShader that displaces/blurs/tints where registered
 * LiquidGlassBox elements sit; boxes report rects via
 * onGloballyPositioned, so scrolling content stays correct.
 */

package com.water0.hydration.ui.theme.liquidglass

import android.annotation.SuppressLint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.FloatRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.water0.hydration.ui.theme.GlassConfig
import kotlin.random.Random

/** 0..1 lens params mapped from the apply-on-restart GlassConfig. */
data class LiquidParams(
    val blur: Float,
    val tint: Color,
    val scale: Float = 0.15f,
    val centerDistortion: Float = 0.25f,
    val elevation: Dp = 8.dp,
    val darkness: Float = 0.15f,
    val warpEdges: Float = 0.35f,
    val chromatic: Float = 1f
)

/** Pure mapping, unit-tested. Blur 24dp saturates the 0..1 range. */
fun GlassConfig.toLiquidParams(
    tintBase: Color,
    scale: Float = 0.15f
): LiquidParams = LiquidParams(
    blur = (blurRadius.value / 24f).coerceIn(0f, 1f),
    tint = tintBase.copy(alpha = tintAlpha),
    scale = scale
)

data class GlassElement(
    val id: String,
    val position: Offset,
    val size: Size,
    val scale: Float,
    val blur: Float,
    val centerDistortion: Float,
    val cornerRadius: Float,
    val elevation: Float,
    val tint: Color,
    val darkness: Float,
    val warpEdges: Float,
    val chromatic: Float,
) {
    fun equalsWithTolerance(other: GlassElement): Boolean {
        if (id != other.id) return false
        val tolerance = 0.01f
        val positionDiff = (position - other.position)
        val positionDistance =
            kotlin.math.sqrt(positionDiff.x * positionDiff.x + positionDiff.y * positionDiff.y)
        return positionDistance < tolerance &&
            kotlin.math.abs(size.width - other.size.width) < tolerance &&
            kotlin.math.abs(size.height - other.size.height) < tolerance &&
            kotlin.math.abs(scale - other.scale) < tolerance &&
            kotlin.math.abs(blur - other.blur) < tolerance &&
            kotlin.math.abs(centerDistortion - other.centerDistortion) < tolerance &&
            kotlin.math.abs(cornerRadius - other.cornerRadius) < tolerance &&
            kotlin.math.abs(elevation - other.elevation) < tolerance &&
            kotlin.math.abs(darkness - other.darkness) < tolerance &&
            kotlin.math.abs(warpEdges - other.warpEdges) < tolerance &&
            kotlin.math.abs(chromatic - other.chromatic) < tolerance &&
            tint == other.tint
    }
}

interface GlassScope {
    fun upsertElement(element: GlassElement)
    fun unregisterElement(id: String)
    fun refreshElementParams(
        id: String,
        tint: Color,
        darkness: Float,
        warpEdges: Float,
        chromatic: Float,
        blur: Float,
        scale: Float,
        centerDistortion: Float,
    )
    fun Modifier.glassBackground(
        id: Long,
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp = 0.dp,
        tint: Color = Color.Transparent,
        darkness: Float = 0f,
        warpEdges: Float = 0f,
        chromatic: Float = 1f,
    ): Modifier
}

interface GlassBoxScope : BoxScope, GlassScope

/** Hoisted lens scope, provided around BOTH container layers so
 * content-layer cards (graphs, buttons) register lenses just like
 * glass-layer chrome. Null in tests/previews -> plain frost. */
val LocalGlassScope =
    androidx.compose.runtime.compositionLocalOf<GlassScope?> { null }

@Composable
fun GlassScope.LiquidGlassBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    params: LiquidParams,
    shape: CornerBasedShape = RoundedCornerShape(0.dp),
    chromatic: Float = 1f,
    content: @Composable BoxScope.() -> Unit = { },
) {
    val id = remember { Random.nextLong() }
    val elementId = "glass_$id"
    // Explicit lifetime: disposal unregisters, so the container never
    // has to guess (the old mark-and-sweep wiped static chrome on
    // every scroll-triggered recomposition).
    DisposableEffect(elementId) {
        onDispose { unregisterElement(elementId) }
    }
    // Param refresh without a layout change: Glass Lab retunes live.
    SideEffect {
        refreshElementParams(
            elementId, params.tint,
            params.darkness.coerceIn(0f, 1f),
            params.warpEdges.coerceIn(0f, 1f),
            chromatic, params.blur.coerceIn(0f, 1f),
            params.scale.coerceIn(0f, 1f),
            params.centerDistortion.coerceIn(0f, 1f)
        )
    }
    Box(
        modifier = modifier.glassBackground(
            id,
            params.scale.coerceIn(0f, 1f),
            params.blur.coerceIn(0f, 1f),
            params.centerDistortion.coerceIn(0f, 1f),
            shape,
            params.elevation,
            params.tint,
            params.darkness.coerceIn(0f, 1f),
            params.warpEdges.coerceIn(0f, 1f),
            chromatic
        ),
        contentAlignment, propagateMinConstraints, content
    )
}

private class GlassBoxScopeImpl(
    boxScope: BoxScope,
    glassScope: GlassScope
) : GlassBoxScope, BoxScope by boxScope,
    GlassScope by glassScope

private class GlassScopeImpl(private val density: Density) : GlassScope {

    var updateCounter by mutableStateOf(0)
    val elements: MutableList<GlassElement> = mutableListOf()

    override fun upsertElement(element: GlassElement) {
        val i = elements.indexOfFirst { it.id == element.id }
        if (i == -1) {
            elements.add(element)
            updateCounter++
        } else if (!elements[i].equalsWithTolerance(element)) {
            elements[i] = element
            updateCounter++
        }
    }

    override fun unregisterElement(id: String) {
        if (elements.removeAll { it.id == id }) updateCounter++
    }

    override fun refreshElementParams(
        id: String,
        tint: Color,
        darkness: Float,
        warpEdges: Float,
        chromatic: Float,
        blur: Float,
        scale: Float,
        centerDistortion: Float,
    ) {
        val i = elements.indexOfFirst { it.id == id }
        if (i == -1) return
        val e = elements[i]
        if (e.tint != tint || e.darkness != darkness ||
            e.warpEdges != warpEdges || e.chromatic != chromatic ||
            e.blur != blur || e.scale != scale ||
            e.centerDistortion != centerDistortion
        ) {
            elements[i] = e.copy(
                tint = tint, darkness = darkness,
                warpEdges = warpEdges, chromatic = chromatic,
                blur = blur, scale = scale,
                centerDistortion = centerDistortion
            )
            updateCounter++
        }
    }

    override fun Modifier.glassBackground(
        id: Long,
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp,
        tint: Color,
        darkness: Float,
        warpEdges: Float,
        chromatic: Float,
    ): Modifier = this
        .background(color = Color.Transparent, shape = shape)
        .onGloballyPositioned { coordinates ->
            val elementId = "glass_$id"
            val position = coordinates.positionInRoot()
            val size = coordinates.size.toSize()

            upsertElement(
                GlassElement(
                    id = elementId,
                    position = position,
                    size = size,
                    cornerRadius = shape.topStart.toPx(size, density),
                    scale = scale,
                    blur = blur,
                    centerDistortion = centerDistortion,
                    elevation = with(density) { elevation.toPx() },
                    tint = tint,
                    darkness = darkness,
                    warpEdges = warpEdges,
                    chromatic = chromatic,
                )
            )
        }
}

/** Fallback for API < 33: gradient glass simulation, no backdrop blur. */
private class GlassScopeFallbackImpl(private val density: Density) : GlassScope {

    override fun upsertElement(element: GlassElement) = Unit
    override fun unregisterElement(id: String) = Unit
    override fun refreshElementParams(
        id: String,
        tint: Color,
        darkness: Float,
        warpEdges: Float,
        chromatic: Float,
        blur: Float,
        scale: Float,
        centerDistortion: Float,
    ) = Unit

    override fun Modifier.glassBackground(
        id: Long,
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp,
        tint: Color,
        darkness: Float,
        warpEdges: Float,
        chromatic: Float,
    ): Modifier {
        val glassTint = if (tint == Color.Transparent) {
            Color.White.copy(alpha = 0.1f)
        } else {
            tint.copy(alpha = (tint.alpha * 0.9f).coerceIn(0f, 1f))
        }

        val darknessOverlay = if (darkness > 0f) {
            Color.Black.copy(alpha = darkness * 0.3f)
        } else {
            Color.Transparent
        }

        val glassGradient = Brush.verticalGradient(
            colors = listOf(
                glassTint,
                glassTint.copy(alpha = glassTint.alpha * 0.7f),
                glassTint.copy(alpha = glassTint.alpha * 0.5f),
                glassTint
            )
        )

        return this
            .background(brush = glassGradient, shape = shape)
            .let { modifier ->
                if (darknessOverlay != Color.Transparent) {
                    modifier.background(color = darknessOverlay, shape = shape)
                } else {
                    modifier
                }
            }
            .let { modifier ->
                if (scale > 0f) {
                    modifier.graphicsLayer {
                        scaleX = 1f + (scale * 0.1f)
                        scaleY = 1f + (scale * 0.1f)
                    }
                } else {
                    modifier
                }
            }
            .let { modifier ->
                if (warpEdges > 0f) {
                    modifier.alpha(1f - (warpEdges * 0.2f).coerceIn(0f, 0.8f))
                } else {
                    modifier
                }
            }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LiquidGlassContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    // Overscroll stretch killed for BOTH layers: lists live in content
    // but the kill used to sit in the glass layer, so flings stretched
    // content while the lens stayed put.
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.foundation.LocalOverscrollConfiguration provides null
    ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val glassScope = remember { GlassScopeImpl(density) }
        androidx.compose.runtime.CompositionLocalProvider(
            LocalGlassScope provides glassScope
        ) {
            GlassContainerWithShader(modifier, content, glassContent, glassScope)
        }
    } else {
        val glassScope = remember { GlassScopeFallbackImpl(density) }
        androidx.compose.runtime.CompositionLocalProvider(
            LocalGlassScope provides glassScope
        ) {
            GlassContainerFallback(modifier, content, glassContent, glassScope)
        }
    }
    } // overscroll-kill provider
}

@SuppressLint("NewApi")
@Composable
private fun GlassContainerWithShader(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
    glassScope: GlassScopeImpl,
) {
    val shader = remember(glassScope.updateCounter) {
        RuntimeShader(GLASS_DISPLACEMENT_SHADER)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                shader.setFloatUniform("resolution", size.width, size.height)
                @Suppress("UNUSED_EXPRESSION")
                glassScope.updateCounter

                val elements = glassScope.elements

                val maxElements = 48
                val positions = FloatArray(maxElements * 2)
                val sizes = FloatArray(maxElements * 2)
                val scales = FloatArray(maxElements)
                val radii = FloatArray(maxElements)
                val elevations = FloatArray(maxElements)
                val centerDistortions = FloatArray(maxElements)
                val tints = FloatArray(maxElements * 4)
                val darkness = FloatArray(maxElements)
                val warpEdges = FloatArray(maxElements)
                val blurs = FloatArray(maxElements)
                val chromatics = FloatArray(maxElements)

                val elementsCount = minOf(elements.size, maxElements)
                shader.setIntUniform("elementsCount", elementsCount)

                for (i in 0 until elementsCount) {
                    val element = elements[i]
                    positions[i * 2] = element.position.x
                    positions[i * 2 + 1] = element.position.y
                    sizes[i * 2] = element.size.width
                    sizes[i * 2 + 1] = element.size.height
                    scales[i] = element.scale
                    radii[i] = element.cornerRadius
                    elevations[i] = element.elevation
                    centerDistortions[i] = element.centerDistortion

                    tints[i * 4] = element.tint.red
                    tints[i * 4 + 1] = element.tint.green
                    tints[i * 4 + 2] = element.tint.blue
                    tints[i * 4 + 3] = element.tint.alpha

                    darkness[i] = element.darkness
                    warpEdges[i] = element.warpEdges
                    blurs[i] = element.blur
                    chromatics[i] = element.chromatic
                }

                shader.setFloatUniform("glassPositions", positions)
                shader.setFloatUniform("glassSizes", sizes)
                shader.setFloatUniform("glassScales", scales)
                shader.setFloatUniform("cornerRadii", radii)
                shader.setFloatUniform("elevations", elevations)
                shader.setFloatUniform("centerDistortions", centerDistortions)
                shader.setFloatUniform("glassTints", tints)
                shader.setFloatUniform("glassDarkness", darkness)
                shader.setFloatUniform("glassWarpEdges", warpEdges)
                shader.setFloatUniform("glassBlurs", blurs)
                shader.setFloatUniform("chromaticFlags", chromatics)

                // Full-screen-lens smear source is gone (glow is
                // matchParentSize now), so the lens is back on.
                renderEffect = RenderEffect.createRuntimeShaderEffect(
                    shader, "contents"
                ).asComposeRenderEffect()
            }
    ) {
        content()
    }
    Box(modifier = modifier) {
        GlassBoxScopeImpl(this, glassScope).glassContent()
    }
}

@Composable
private fun GlassContainerFallback(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
    glassScope: GlassScopeFallbackImpl,
) {
    Box(modifier = modifier) {
        content()
    }
    Box(modifier = modifier) {
        GlassBoxScopeImpl(this, glassScope).glassContent()
    }
}

private val GLASS_DISPLACEMENT_SHADER = """
    uniform float2 resolution;
    uniform shader contents;
    uniform int elementsCount;
    uniform float2 glassPositions[48];
    uniform float2 glassSizes[48];
    uniform float glassScales[48];
    uniform float cornerRadii[48];
    uniform float elevations[48];
    uniform float centerDistortions[48];
    uniform float glassTints[192]; // 48 elements * 4 components (r,g,b,a)
    uniform float glassDarkness[48];
    uniform float glassWarpEdges[48];
    uniform float glassBlurs[48];
    uniform float chromaticFlags[48];

    float sdfRoundedRect(float2 p, float2 halfSize, float radius) {
        float2 d = abs(p) - halfSize + radius;
        return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - radius;
    }

    float getWarpRegion(float2 localCoord, float2 halfSize, float cornerRadius, float warpEdges) {
        if (warpEdges <= 0.0) return 0.0;

        float outerSdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);
        if (outerSdf >= 0.0) return 0.0;

        float inset = warpEdges * min(halfSize.x, halfSize.y) * 0.5;
        float2 innerSize = max(halfSize - inset, 0.1);
        float innerRadius = max(cornerRadius * min(innerSize.x / halfSize.x, innerSize.y / halfSize.y), 0.0);

        float innerSdf = sdfRoundedRect(localCoord, innerSize, innerRadius);
        return step(0.0, innerSdf);
    }

    float2 applyWarpDistortion(float2 localCoord, float2 halfSize, float cornerRadius, float warpEdges) {
        if (warpEdges <= 0.0) return localCoord;

        float inset = warpEdges * min(halfSize.x, halfSize.y) * 0.5;
        float2 innerSize = max(halfSize - inset, 0.1);
        float innerRadius = max(cornerRadius * min(innerSize.x / halfSize.x, innerSize.y / halfSize.y), 0.0);

        float innerSdf = sdfRoundedRect(localCoord, innerSize, innerRadius);
        if (innerSdf <= 0.0) return localCoord;

        float normalizedDist = clamp(innerSdf / inset, 0.0, 1.0);
        float warpIntensity = normalizedDist * normalizedDist * warpEdges;

        float pullStrength = warpIntensity * 0.8;
        float targetScale = max(0.1, 1.0 - pullStrength);
        float2 pulledCoord = localCoord * targetScale;

        float2 centerDir = normalize(localCoord);
        float2 radialOffset = centerDir * (warpIntensity * 0.03 * length(localCoord));

        if (warpEdges > 0.7 && normalizedDist > 0.8) {
            float angle = atan(localCoord.y, localCoord.x) + normalizedDist * warpEdges * 0.5;
            float r = length(pulledCoord);
            pulledCoord = float2(cos(angle), sin(angle)) * r;
        }

        return pulledCoord + radialOffset;
    }

    float2 applyLensEffect(float2 fragCoord, float2 center, float2 size, float cornerRadius, float scale, float centerDistortion) {
        if (scale <= 0.0) return fragCoord;

        float2 localCoord = fragCoord - center;
        float2 halfSize = size * 0.5;

        float sdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);
        if (sdf >= 0.0) return fragCoord;

        float2 rel = localCoord / halfSize;
        float normalizedDist = length(rel) / 1.414;

        float baseScale = 1.0 + scale;
        float distortionFactor = 1.0;

        if (centerDistortion > 0.0) {
            float profile = 1.0 - smoothstep(0.0, 1.0, normalizedDist);
            distortionFactor = 1.0 + centerDistortion * profile;
        }

        float finalScale = baseScale * distortionFactor;
        return center + (fragCoord - center) / finalScale;
    }

    float getShadowIntensity(float2 localCoord, float2 halfSize, float cornerRadius, float elevation) {
        if (elevation <= 0.0) return 0.0;

        float shadowOffset = elevation * 0.5;
        float shadowBlur = elevation * 2.0;

        float2 shadowCoord = localCoord - float2(0.0, shadowOffset);
        float shadowSdf = sdfRoundedRect(shadowCoord, halfSize, cornerRadius);
        float originalSdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);

        if (originalSdf <= 0.0 || shadowSdf > shadowBlur) return 0.0;

        return (1.0 - shadowSdf / shadowBlur) * 0.15;
    }

    float getRimHighlight(float2 localCoord, float2 halfSize, float cornerRadius) {
        float sdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);
        float rimWidth = 5.0;

        if (sdf <= 0.0 || sdf >= rimWidth) return 0.0;

        float intensity = (rimWidth - sdf) / rimWidth;
        float verticalPos = localCoord.y / halfSize.y;
        float lightingFactor = mix(1.2, 0.7, (verticalPos + 1.0) * 0.5);

        return intensity * 0.8 * lightingFactor;
    }

    float4 main(float2 fragCoord) {
        float2 finalCoord = fragCoord;
        float shadowAlpha = 0.0;
        float rimHighlight = 0.0;
        float4 tintColor = float4(0.0);
        float darknessEffect = 0.0;
        float blurRadius = 0.0;
        float2 surfaceNormal = float2(0.0);

        for (int i = 0; i < 48; i++) {
            if (i >= elementsCount) break;
            float2 center = glassPositions[i] + glassSizes[i] * 0.5;
            float2 localCoord = fragCoord - center;
            float2 halfSize = glassSizes[i] * 0.5;
            float cornerRadius = cornerRadii[i];

            float sdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);

            if (sdf < 0.0 && glassBlurs[i] > 0.0) {
                blurRadius = max(blurRadius, glassBlurs[i] * 20.0);
            }

            float warpRegion = getWarpRegion(localCoord, halfSize, cornerRadius, glassWarpEdges[i]);
            if (warpRegion > 0.0) {
                float2 warpedCoord = applyWarpDistortion(localCoord, halfSize, cornerRadius, glassWarpEdges[i]);
                float2 warpedFragCoord = center + warpedCoord;
                finalCoord = applyLensEffect(warpedFragCoord, glassPositions[i] + glassSizes[i] * 0.5,
                                           glassSizes[i], cornerRadius, glassScales[i], centerDistortions[i]);
            } else {
                finalCoord = applyLensEffect(finalCoord, center, glassSizes[i], cornerRadius,
                                           glassScales[i], centerDistortions[i]);
            }

            shadowAlpha = max(shadowAlpha, getShadowIntensity(localCoord, halfSize, cornerRadius, elevations[i]));
            rimHighlight = max(rimHighlight, getRimHighlight(localCoord, halfSize, cornerRadius));

            if (sdf > 0.0 && sdf < 4.0 && surfaceNormal.x == 0.0 && surfaceNormal.y == 0.0) {
                float epsilon = 1.0;
                float sdfX = sdfRoundedRect(localCoord + float2(epsilon, 0.0), halfSize, cornerRadius);
                float sdfY = sdfRoundedRect(localCoord + float2(0.0, epsilon), halfSize, cornerRadius);
                surfaceNormal = normalize(float2(sdfX - sdf, sdfY - sdf));
            }

            if (sdf < 0.0) {
                float4 elementTint = float4(glassTints[i * 4], glassTints[i * 4 + 1],
                                          glassTints[i * 4 + 2], glassTints[i * 4 + 3]);
                if (elementTint.a > 0.0) {
                    tintColor = mix(tintColor, elementTint, elementTint.a);
                }

                float currentDarkness = glassDarkness[i];
                if (currentDarkness > 0.0) {
                    float maxRadius = min(halfSize.x, halfSize.y) * 0.8;
                    float distanceFromEdge = abs(sdf);
                    if (distanceFromEdge < maxRadius) {
                        float intensity = smoothstep(0.0, 1.0, (maxRadius - distanceFromEdge) / maxRadius);
                        darknessEffect = max(darknessEffect, currentDarkness * intensity);
                    }
                }
            }
        }

        // Diffraction: where the lens bent the ray, split RGB along the
        // bend direction (prismatic fringe, iPhone-style). Still water
        // (no bend) samples once — zero extra cost outside glass.
        float2 bendVec = finalCoord - fragCoord;
        float bendLen = length(bendVec);
        // Per-surface diffraction gate: only elements opted in split
        // the ray; the rest refract clean.
        float wantChroma = 0.0;
        for (int j = 0; j < 48; j++) {
            if (j >= elementsCount) break;
            float2 c = glassPositions[j] + glassSizes[j] * 0.5;
            float2 hSize = glassSizes[j] * 0.5;
            if (abs(fragCoord.x - c.x) < hSize.x && abs(fragCoord.y - c.y) < hSize.y) {
                wantChroma = max(wantChroma, chromaticFlags[j]);
            }
        }
        float4 color;
        if (bendLen > 0.5 && wantChroma > 0.5) {
            float2 bendDir = bendVec / bendLen;
            float caPx = min(bendLen * 0.12, 2.5);
            float r = contents.eval(finalCoord + bendDir * caPx).r;
            float g = contents.eval(finalCoord).g;
            float b = contents.eval(finalCoord - bendDir * caPx).b;
            float a = contents.eval(finalCoord).a;
            color = float4(r, g, b, a);
        } else {
            color = contents.eval(finalCoord);
        }

        if (blurRadius > 0.0) {
            float4 blurredColor = float4(0.0);
            float totalWeight = 0.0;
            float invRadius = 1.0 / max(blurRadius, 1.0);

            for (int dx = -5; dx <= 5; dx++) {
                for (int dy = -5; dy <= 5; dy++) {
                    float2 offset = float2(float(dx), float(dy)) * blurRadius * 0.4;
                    float distance = length(offset) * invRadius;
                    float weight = exp(-distance * distance * 2.0);
                    blurredColor += contents.eval(finalCoord + offset) * weight;
                    totalWeight += weight;
                }
            }
            color = blurredColor / totalWeight;
        }

        if (tintColor.a > 0.0) {
            color.rgb = mix(color.rgb, tintColor.rgb, tintColor.a * 0.9);
        }

        if (darknessEffect > 0.0) {
            color.rgb = mix(color.rgb, float3(0.0), darknessEffect * 0.5);
        }

        if (rimHighlight > 0.0) {
            float2 reflectionOffset = surfaceNormal * 24.0;
            float4 reflectedColor = contents.eval(fragCoord + reflectionOffset);
            reflectedColor.rgb = max(reflectedColor.rgb * 1.8 + 0.35, 0.15);
            color = mix(color, reflectedColor, rimHighlight);
        }

        if (shadowAlpha > 0.0) {
            color.rgb = mix(color.rgb, float3(0.0), shadowAlpha);
        }

        return color;
    }
""".trimIndent()
