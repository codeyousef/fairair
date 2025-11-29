# Implementation Plan: Velocity UI Redesign

**Branch**: `003-velocity-ui-redesign` | **Date**: 2025-11-29 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-velocity-ui-redesign/spec.md`

## Summary

Transform the flyadeal mobile/web app UI from traditional Material Design forms to the "Velocity Experience" - a modern, immersive interface featuring:

- **Natural language sentence builder** for flight search ("I want to fly from [Riyadh] to [Dubai]...")
- **Glassmorphism design** with deep purple backgrounds and neon lime accents
- **Dynamic destination backgrounds** that change based on selection
- **Slide-up results overlay** with expandable holographic flight cards
- **Full RTL/Arabic support** with proper font switching

This is a **UI-only change** - no backend modifications required. All API contracts remain unchanged.

## Technical Context

**Language/Version**: Kotlin 2.0.21 (K2 compiler)
**Primary Dependencies**: Compose Multiplatform 1.7.1, Voyager 1.1.0, Koin 4.0.0, Ktor Client 3.0.1
**Storage**: N/A (UI-only, uses existing LocalStorage for preferences)
**Testing**: Compose UI tests, manual visual comparison with design.html
**Target Platform**: Android (API 24+), iOS (15+), Web (WASM)
**Project Type**: Mobile multiplatform (Kotlin Multiplatform)
**Performance Goals**: 60fps animations, <100ms interaction feedback, <1s overlay transition
**Constraints**: Cross-platform consistency, offline-capable (after initial load)
**Scale/Scope**: 2 screens (Search, Results) redesigned, ~15 new components

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

The project constitution is not fully defined (template placeholders). Applying standard principles:

| Principle | Status | Notes |
|-----------|--------|-------|
| Simplicity | PASS | Minimal new dependencies, uses existing architecture |
| Testability | PASS | UI components are independently testable |
| Cross-platform | PASS | All implementations use Compose Multiplatform common code |
| RTL Support | PASS | Using CompositionLocalProvider pattern |

No violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/003-velocity-ui-redesign/
├── plan.md              # This file
├── research.md          # Technical research (completed)
├── data-model.md        # UI state models (completed)
├── quickstart.md        # Development guide (completed)
├── contracts/           # API contracts (unchanged, copied)
│   └── openapi.yaml
├── checklists/
│   └── requirements.md  # Spec validation checklist
└── tasks.md             # Implementation tasks (next: /speckit.tasks)
```

### Source Code (repository root)

```text
apps-kmp/
├── src/
│   ├── commonMain/
│   │   ├── composeResources/
│   │   │   ├── font/                    # NEW: Custom fonts
│   │   │   │   ├── space_grotesk_*.ttf
│   │   │   │   └── noto_kufi_arabic_*.ttf
│   │   │   └── drawable/                # NEW: Destination backgrounds
│   │   │       └── bg_*.webp
│   │   │
│   │   └── kotlin/com/flyadeal/app/
│   │       ├── ui/
│   │       │   ├── theme/
│   │       │   │   ├── FlyadealTheme.kt     # EXISTING (preserve)
│   │       │   │   └── VelocityTheme.kt     # NEW: Velocity design system
│   │       │   │
│   │       │   ├── components/
│   │       │   │   ├── velocity/            # NEW: Velocity components
│   │       │   │   │   ├── GlassCard.kt
│   │       │   │   │   ├── SentenceBuilder.kt
│   │       │   │   │   ├── LaunchButton.kt
│   │       │   │   │   ├── VelocityFlightCard.kt
│   │       │   │   │   ├── FareGrid.kt
│   │       │   │   │   └── DestinationBackground.kt
│   │       │   │   └── CommonComponents.kt  # EXISTING (preserve)
│   │       │   │
│   │       │   └── screens/
│   │       │       ├── search/
│   │       │       │   ├── SearchScreen.kt          # MODIFY: Use Velocity
│   │       │       │   └── SearchScreenModel.kt     # MODIFY: Add state
│   │       │       └── results/
│   │       │           ├── ResultsScreen.kt         # MODIFY: Overlay pattern
│   │       │           └── ResultsScreenModel.kt    # MODIFY: Expansion state
│   │       │
│   │       └── localization/
│   │           └── Strings.kt               # MODIFY: Add Velocity strings
│   │
│   ├── androidMain/                         # EXISTING (unchanged)
│   ├── iosMain/                             # EXISTING (unchanged)
│   └── wasmJsMain/                          # EXISTING (unchanged)
│
└── build.gradle.kts                         # EXISTING (unchanged)
```

**Structure Decision**: Using existing Kotlin Multiplatform structure. New Velocity components go in `ui/components/velocity/` to keep them organized. Existing screens are modified in-place with new Velocity implementations.

## Key Technical Decisions

### From Research

| Topic | Decision | Rationale |
|-------|----------|-----------|
| Glassmorphism | Custom Modifier with alpha backgrounds | Cross-platform consistent, no dependencies |
| Fonts | Compose Resources | Built-in support in CMP 1.7+ |
| Sentence Builder | ClickableText + AnnotatedString | Natural text wrapping, inline selection |
| Overlay Animation | AnimatedVisibility + slideInVertically | Declarative, smooth enter/exit |
| Background Images | AsyncImage with crossfade | Async loading with transitions |
| RTL | CompositionLocalProvider | Standard Compose pattern |
| Card Expansion | animateContentSize | Automatic height animation |
| Grid Layout | LazyVerticalGrid + GridCells.Adaptive | Responsive, matches design |
| Launch Button | Custom Canvas glow | Platform-independent glow effect |

### Color Palette

| Name | Hex | Usage |
|------|-----|-------|
| BackgroundDeep | #120521 | App background |
| Accent | #CCFF00 | CTAs, highlights, interactive |
| GlassBg | rgba(255,255,255,0.1) | Card backgrounds |
| GlassBorder | rgba(255,255,255,0.1) | Card borders |
| TextMain | #FFFFFF | Primary text |
| TextMuted | rgba(255,255,255,0.6) | Secondary text |

### Typography

| Style | Font | Weight | Size |
|-------|------|--------|------|
| Hero Title | Space Grotesk | Light (300) | 64sp |
| Sentence | Space Grotesk | Light (300) | 40sp |
| Magic Input | Space Grotesk | Bold (700) | 40sp |
| Flight Path | Space Grotesk | Bold (700) | 48sp |
| Time Big | Space Grotesk | Bold (700) | 32sp |

Arabic uses Noto Kufi Arabic with same weight mappings.

## Complexity Tracking

No complexity violations. This feature:
- Uses existing project structure
- Adds no new dependencies
- Follows established patterns (Voyager navigation, Koin DI, ScreenModel)
- Is UI-only (no backend changes)

## Next Steps

1. Run `/speckit.tasks` to generate implementation tasks
2. Tasks will be organized by user story for independent implementation
3. Start with US1 (Search) and US2 (Results) as P1 priorities

## Related Documents

- [spec.md](./spec.md) - Feature specification
- [research.md](./research.md) - Technical research findings
- [data-model.md](./data-model.md) - UI state models
- [quickstart.md](./quickstart.md) - Development setup guide
- [contracts/openapi.yaml](./contracts/openapi.yaml) - API contracts (unchanged)
