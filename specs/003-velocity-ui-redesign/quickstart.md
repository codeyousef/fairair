# Quickstart: Velocity UI Redesign

**Feature**: 003-velocity-ui-redesign
**Date**: 2025-11-29

## Prerequisites

- JDK 17+
- Android SDK (for Android builds)
- Xcode 15+ (for iOS builds, macOS only)
- Gradle 8.x (wrapper included)

## Development Setup

### 1. Start the Backend

The UI connects to the Spring Boot backend. Start it first:

```bash
./gradlew :backend-spring:bootRun
```

Backend runs on `http://localhost:8080`.

### 2. Run the App

#### Android
```bash
./gradlew :apps-kmp:installDebug
```

#### iOS (macOS only)
```bash
# Build the framework
./gradlew :apps-kmp:iosSimulatorArm64Main

# Open Xcode project
open apps-kmp/iosApp/iosApp.xcodeproj
```

#### Desktop (for testing)
```bash
./gradlew :apps-kmp:run
```

#### Web (WASM)
```bash
./gradlew :apps-kmp:wasmJsBrowserRun
```

## Project Structure

### UI Components (New for Velocity)

```
apps-kmp/src/commonMain/kotlin/com/flyadeal/app/
├── ui/
│   ├── theme/
│   │   ├── FlyadealTheme.kt        # Existing (keep for other screens)
│   │   └── VelocityTheme.kt        # NEW: Velocity design system
│   │
│   ├── components/
│   │   ├── velocity/               # NEW: Velocity-specific components
│   │   │   ├── GlassCard.kt        # Glassmorphism card
│   │   │   ├── SentenceBuilder.kt  # Natural language search input
│   │   │   ├── LaunchButton.kt     # Circular glow button
│   │   │   ├── FlightCard.kt       # Holographic flight result card
│   │   │   ├── FareGrid.kt         # Expandable fare options
│   │   │   └── DestinationBg.kt    # Dynamic background images
│   │   │
│   │   └── CommonComponents.kt     # Existing shared components
│   │
│   └── screens/
│       ├── search/
│       │   ├── SearchScreen.kt           # REPLACE with Velocity design
│       │   ├── SearchScreenModel.kt      # Extend for new state
│       │   └── VelocitySearchScreen.kt   # NEW: Velocity search UI
│       │
│       └── results/
│           ├── ResultsScreen.kt          # REPLACE with overlay
│           ├── ResultsScreenModel.kt     # Extend for expansion state
│           └── VelocityResultsOverlay.kt # NEW: Slide-up overlay
│
├── localization/
│   ├── Strings.kt                  # Add Velocity UI strings
│   └── LocalizationManager.kt      # Existing (unchanged)
│
└── navigation/
    └── AppNavigation.kt            # Update for overlay pattern
```

### Assets (New)

```
apps-kmp/src/commonMain/composeResources/
├── font/
│   ├── space_grotesk_light.ttf     # NEW
│   ├── space_grotesk_regular.ttf   # NEW
│   ├── space_grotesk_medium.ttf    # NEW
│   ├── space_grotesk_semibold.ttf  # NEW
│   ├── space_grotesk_bold.ttf      # NEW
│   ├── noto_kufi_arabic_light.ttf  # NEW
│   ├── noto_kufi_arabic_regular.ttf# NEW
│   ├── noto_kufi_arabic_semibold.ttf# NEW
│   └── noto_kufi_arabic_bold.ttf   # NEW
│
└── drawable/
    ├── bg_jeddah.webp              # NEW: Destination backgrounds
    ├── bg_dubai.webp               # NEW
    ├── bg_cairo.webp               # NEW
    ├── bg_riyadh.webp              # NEW
    └── bg_dammam.webp              # NEW
```

## Key Implementation Files

### 1. VelocityTheme.kt

Core design system with colors, typography, and styling:

```kotlin
// Location: ui/theme/VelocityTheme.kt

object VelocityColors {
    val BackgroundDeep = Color(0xFF120521)
    val Accent = Color(0xFFCCFF00)
    val GlassBg = Color.White.copy(alpha = 0.1f)
    val GlassBorder = Color.White.copy(alpha = 0.1f)
    val TextMain = Color.White
    val TextMuted = Color.White.copy(alpha = 0.6f)
}

@Composable
fun VelocityTheme(
    isRtl: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalVelocityColors provides VelocityColors,
        // ... typography, spacing
    ) {
        content()
    }
}
```

### 2. SentenceBuilder.kt

Natural language search interface:

```kotlin
// Location: ui/components/velocity/SentenceBuilder.kt

@Composable
fun SentenceBuilder(
    origin: String?,
    destination: String?,
    date: String,
    passengers: Int,
    onOriginClick: () -> Unit,
    onDestinationClick: () -> Unit,
    onDateClick: () -> Unit,
    onPassengersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // "I want to fly from [Origin] to [Destination] departing on [Date] with [Passengers]."
}
```

### 3. VelocityResultsOverlay.kt

Full-screen slide-up results:

```kotlin
// Location: ui/screens/results/VelocityResultsOverlay.kt

@Composable
fun VelocityResultsOverlay(
    isVisible: Boolean,
    flights: List<VelocityFlightCard>,
    expandedFlightId: String?,
    onFlightClick: (String) -> Unit,
    onFareSelect: (String, FareFamilyCode) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        // Results content
    }
}
```

## Testing the Implementation

### Visual Comparison

1. Open `design.html` in browser as reference
2. Run app on target platform
3. Compare side-by-side:
   - Colors match (deep purple background, neon lime accents)
   - Sentence builder works with inline selections
   - Results overlay slides up from bottom
   - Cards have glassmorphism effect
   - Fare grid expands on tap
   - RTL works correctly in Arabic

### Functional Testing

```bash
# Run all tests
./gradlew test

# Run specific UI tests (when added)
./gradlew :apps-kmp:connectedAndroidTest
```

### Checklist

- [ ] Sentence builder displays with tappable fields
- [ ] Origin selection shows available airports
- [ ] Destination filters based on origin
- [ ] Date picker works on all platforms
- [ ] Launch button disabled until destination selected
- [ ] Launch button has glow effect when enabled
- [ ] Results overlay slides up smoothly
- [ ] Flight cards display correctly
- [ ] Tapping card expands fare grid
- [ ] Only one card expanded at a time
- [ ] Fare selection highlights correctly
- [ ] Close button dismisses overlay
- [ ] Language toggle switches to Arabic
- [ ] Layout reverses in RTL mode
- [ ] Arabic font displays correctly
- [ ] Background images fade based on destination

## Troubleshooting

### Fonts not loading
Ensure fonts are in `composeResources/font/` and referenced correctly in `VelocityTypography`.

### Background images not showing
Check image paths in `DestinationTheme` and verify files exist in `composeResources/drawable/`.

### Glassmorphism looks different per platform
This is expected. Backdrop blur has limited support on some platforms. The semi-transparent overlay will always work.

### RTL layout not mirroring
Ensure `CompositionLocalProvider` with `LocalLayoutDirection` wraps the entire screen.

### Animations janky on Android emulator
Test on real device. Emulator performance varies significantly.
