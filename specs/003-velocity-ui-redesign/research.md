# Research: Velocity UI Redesign

**Feature**: 003-velocity-ui-redesign
**Date**: 2025-11-29

## Research Topics

### 1. Compose Multiplatform Glassmorphism Implementation

**Decision**: Use custom Modifier extensions with `graphicsLayer` for blur effects and semi-transparent backgrounds

**Rationale**:
- Compose Multiplatform doesn't have native glassmorphism support
- `Modifier.blur()` is available but limited on some platforms
- Custom approach using `graphicsLayer` with alpha and shadow provides cross-platform consistency
- Background blur can be simulated with layered gradients and semi-transparent overlays

**Alternatives Considered**:
- Native platform blur APIs (rejected: not cross-platform)
- Third-party blur libraries (rejected: adds dependencies, may not work on WASM)
- CSS-style backdrop-filter (rejected: only works on web)

**Implementation Pattern**:
```kotlin
fun Modifier.glassmorphism() = this
    .background(
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp)
    )
    .border(
        width = 1.dp,
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp)
    )
```

---

### 2. Custom Font Loading in Compose Multiplatform

**Decision**: Use Compose Resources for font loading with platform-specific fallbacks

**Rationale**:
- Space Grotesk and Noto Kufi Arabic need to be loaded as custom fonts
- Compose Multiplatform 1.7+ supports resource-based font loading
- Fonts placed in `commonMain/composeResources/font/` are accessible cross-platform

**Alternatives Considered**:
- System fonts only (rejected: Space Grotesk not available by default)
- Platform-specific font loading (rejected: code duplication)
- Google Fonts API (rejected: requires network, not offline-capable)

**Implementation Pattern**:
```kotlin
val SpaceGrotesk = FontFamily(
    Font(Res.font.space_grotesk_regular, FontWeight.Normal),
    Font(Res.font.space_grotesk_bold, FontWeight.Bold)
)
```

---

### 3. Sentence Builder UI Pattern

**Decision**: Use inline `ClickableText` with annotated strings and dropdown menus

**Rationale**:
- Natural language interface requires inline selectable elements
- `ClickableText` with `AnnotatedString` allows styling specific spans
- Dropdown menus (or bottom sheets on mobile) provide selection UI
- This pattern matches the HTML hidden-select overlay approach

**Alternatives Considered**:
- Multiple Text composables with clickable modifiers (rejected: harder to wrap text naturally)
- WebView with HTML (rejected: defeats KMP purpose)
- Custom text layout (rejected: over-engineering)

**Implementation Pattern**:
```kotlin
@Composable
fun SentenceBuilder(
    origin: String?,
    destination: String?,
    date: String?,
    passengers: Int,
    onOriginClick: () -> Unit,
    onDestinationClick: () -> Unit,
    onDateClick: () -> Unit,
    onPassengersClick: () -> Unit
) {
    // Row with wrapping text and inline clickable spans
}
```

---

### 4. Full-Screen Overlay Animation

**Decision**: Use `AnimatedVisibility` with `slideInVertically`/`slideOutVertically` transitions

**Rationale**:
- Results overlay needs to slide up from bottom (matching design.html)
- `AnimatedVisibility` handles enter/exit animations declaratively
- `slideInVertically` with `initialOffsetY = { it }` slides from bottom
- Combined with `fadeIn`/`fadeOut` for smooth transition

**Alternatives Considered**:
- Custom animation with `animate*AsState` (rejected: more complex)
- Navigation transitions (rejected: results is an overlay, not a screen)
- Platform-specific sheet APIs (rejected: not cross-platform consistent)

**Implementation Pattern**:
```kotlin
AnimatedVisibility(
    visible = showResults,
    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
) {
    ResultsOverlay(...)
}
```

---

### 5. Dynamic Background Image Loading

**Decision**: Use Coil/Kamel for async image loading with crossfade animation

**Rationale**:
- Destination backgrounds need async loading with fade transitions
- Coil3 has Compose Multiplatform support (experimental)
- Images can be bundled or loaded from CDN
- Crossfade animation provides smooth background transitions

**Alternatives Considered**:
- Bundled images only (rejected: larger app size)
- Platform-specific image loaders (rejected: code duplication)
- Manual bitmap loading (rejected: complex, no caching)

**Implementation Pattern**:
```kotlin
@Composable
fun DestinationBackground(
    destinationCode: String?,
    modifier: Modifier = Modifier
) {
    val imageUrl = destinationCode?.let { getBackgroundUrl(it) }

    AnimatedVisibility(visible = imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.graphicsLayer { alpha = 0.4f }
        )
    }
}
```

---

### 6. RTL Layout Support in Compose

**Decision**: Use `CompositionLocalProvider` with `LocalLayoutDirection` and separate string resources

**Rationale**:
- Compose supports RTL via `LocalLayoutDirection`
- Automatic layout mirroring for Row, padding, etc.
- Font family switches via composition local
- String resources already exist in the codebase (LocalizationManager)

**Alternatives Considered**:
- Manual layout direction handling (rejected: error-prone)
- Separate composables for RTL (rejected: code duplication)

**Implementation Pattern**:
```kotlin
CompositionLocalProvider(
    LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    LocalFontFamily provides if (isRtl) NotoKufiArabic else SpaceGrotesk
) {
    content()
}
```

---

### 7. Expandable Card Animation

**Decision**: Use `animateContentSize` modifier with custom animation spec

**Rationale**:
- Flight cards need smooth expansion to show fare grid
- `animateContentSize()` handles height changes automatically
- Combined with state management for single-expanded-card behavior
- Matches the HTML toggle pattern

**Implementation Pattern**:
```kotlin
var expandedCardId by remember { mutableStateOf<String?>(null) }

Card(
    modifier = Modifier.animateContentSize(
        animationSpec = spring(dampingRatio = 0.8f)
    )
) {
    // Card content
    if (isExpanded) {
        FareGrid(...)
    }
}
```

---

### 8. Color System Migration

**Decision**: Create new `VelocityColors` object alongside existing `FlyadealColors`, with gradual migration

**Rationale**:
- New design uses different color palette (deep purple #120521, neon lime #ccff00)
- Existing colors still needed for other screens during transition
- Material3 ColorScheme can be rebuilt with velocity colors
- Theme switcher allows A/B testing if needed

**Color Mapping**:
| Design.html | Compose Color |
|-------------|---------------|
| --bg-deep (#120521) | VelocityColors.BackgroundDeep |
| --accent (#ccff00) | VelocityColors.Accent |
| --bg-glass (rgba 255,255,255,0.1) | VelocityColors.GlassBg |
| --border-glass (rgba 255,255,255,0.1) | VelocityColors.GlassBorder |
| --text-main (#ffffff) | VelocityColors.TextMain |
| --text-muted (rgba 255,255,255,0.6) | VelocityColors.TextMuted |

---

### 9. Responsive Grid Layout

**Decision**: Use `LazyVerticalGrid` with `GridCells.Adaptive(minSize = 300.dp)`

**Rationale**:
- Flight cards need responsive grid (auto-fill, min 300px from design)
- `LazyVerticalGrid` with adaptive cells provides this behavior
- Works consistently across screen sizes and platforms

**Implementation Pattern**:
```kotlin
LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 300.dp),
    contentPadding = PaddingValues(24.dp),
    horizontalArrangement = Arrangement.spacedBy(24.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
) {
    items(flights) { flight ->
        FlightCard(flight = flight)
    }
}
```

---

### 10. Circular Launch Button with Glow Effect

**Decision**: Custom composable with `Box`, `Canvas` for glow, and scale animation on press

**Rationale**:
- Design requires 80px circular button with neon glow
- Glow effect achieved via `drawBehind` with radial gradient
- Scale animation on hover/press using `animateFloatAsState`

**Implementation Pattern**:
```kotlin
@Composable
fun LaunchButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (pressed) 1.1f else 1f)

    Box(
        modifier = Modifier
            .size(80.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawBehind {
                // Glow effect
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VelocityColors.Accent.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    radius = size.minDimension * 0.8f
                )
            }
            .background(
                color = if (enabled) VelocityColors.Accent else Color.Gray,
                shape = CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Search",
            tint = Color.Black
        )
    }
}
```

---

## Summary

All technical unknowns have been resolved. The implementation will use:

1. **Glassmorphism**: Custom Modifier with semi-transparent backgrounds and borders
2. **Fonts**: Compose Resources for Space Grotesk and Noto Kufi Arabic
3. **Sentence Builder**: ClickableText with AnnotatedString and dropdown menus
4. **Overlay Animation**: AnimatedVisibility with slideInVertically
5. **Background Images**: Coil/Kamel for async loading with crossfade
6. **RTL Support**: CompositionLocalProvider with LocalLayoutDirection
7. **Card Expansion**: animateContentSize modifier
8. **Colors**: New VelocityColors object with Velocity design palette
9. **Grid Layout**: LazyVerticalGrid with GridCells.Adaptive
10. **Launch Button**: Custom composable with Canvas glow effect

No external library additions required beyond existing dependencies. All patterns are compatible with Android, iOS, and WASM targets.
