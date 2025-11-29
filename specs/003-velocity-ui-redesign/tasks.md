# Tasks: Velocity UI Redesign

**Input**: Design documents from `/specs/003-velocity-ui-redesign/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, quickstart.md ✓, contracts/ ✓

**Tests**: Tests are NOT included in this task list (not explicitly requested). Add test tasks if TDD approach is desired.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Based on plan.md structure:
- **Source**: `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/`
- **Resources**: `apps-kmp/src/commonMain/composeResources/`
- **Theme**: `ui/theme/`
- **Components**: `ui/components/velocity/`
- **Screens**: `ui/screens/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Font assets and resource foundation

- [ ] T001 [P] Download Space Grotesk fonts (Light, Regular, Medium, SemiBold, Bold) to `apps-kmp/src/commonMain/composeResources/font/`
- [ ] T002 [P] Download Noto Kufi Arabic fonts (Light, Regular, SemiBold, Bold) to `apps-kmp/src/commonMain/composeResources/font/`
- [ ] T003 [P] Add destination background images (JED, DXB, CAI, RUH, DMM) as WebP to `apps-kmp/src/commonMain/composeResources/drawable/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core design system and state models that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 Create VelocityColors object with BackgroundDeep, Accent, GlassBg, GlassBorder, TextMain, TextMuted in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/theme/VelocityColors.kt`
- [ ] T005 Create VelocityTypography with Space Grotesk font family definitions (heroTitle, sentenceBuilder, magicInput, flightPath, timeBig, priceDisplay, labelSmall) in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/theme/VelocityTypography.kt`
- [ ] T006 Create VelocityTheme composable with CompositionLocalProvider for colors, typography, and RTL direction in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/theme/VelocityTheme.kt`
- [ ] T007 [P] Create Modifier.glassmorphism() extension for glass card styling in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/GlassMorphism.kt`
- [ ] T008 [P] Create GlassCard composable using glassmorphism modifier in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/GlassCard.kt`
- [ ] T009 [P] Create SearchField enum (ORIGIN, DESTINATION, DATE, PASSENGERS) in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/search/SearchField.kt`
- [ ] T010 [P] Create VelocitySearchState data class with selectedOrigin, selectedDestination, departureDate, passengerCount, activeField, destinationBackground in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/search/VelocitySearchState.kt`
- [ ] T011 [P] Create VelocityFlightCard data class with id, times, codes, duration, lowestPrice, fareFamilies, isExpanded in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/results/VelocityFlightCard.kt`
- [ ] T012 [P] Create FareFamily data class and FareFamilyCode enum (FLY, FLY_PLUS, FLY_MAX) in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/results/FareFamily.kt`
- [ ] T013 [P] Create VelocityResultsState data class with isVisible, flights, expandedFlightId, selectedFlight, selectedFare, isLoading, error in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/results/VelocityResultsState.kt`
- [ ] T014 [P] Create DestinationTheme data class and predefined destination mappings (JED, DXB, CAI, RUH, DMM) in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/DestinationTheme.kt`
- [ ] T015 Add Velocity UI strings to Strings.kt (English) - search sentence, fare names, button labels in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/localization/Strings.kt`
- [ ] T016 Add Velocity UI strings to Strings.kt (Arabic) - search sentence, fare names, button labels in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/localization/Strings.kt`

**Checkpoint**: Foundation ready - VelocityTheme, state models, and strings available for all user stories

---

## Phase 3: User Story 1 - Natural Language Flight Search (Priority: P1) 🎯 MVP

**Goal**: Implement the conversational sentence-builder search interface with tappable fields and circular launch button

**Independent Test**: User can complete a flight search by tapping on highlighted words in a sentence, selecting values from dropdowns, and launching search - the search request is sent to the backend.

### Implementation for User Story 1

- [ ] T017 [P] [US1] Create SentenceBuilder composable with ClickableText and AnnotatedString for inline tappable fields in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/SentenceBuilder.kt`
- [ ] T018 [P] [US1] Create MagicInputField composable for highlighted selectable text spans with accent underline in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/MagicInputField.kt`
- [ ] T019 [P] [US1] Create LaunchButton composable with circular shape, glow effect (Canvas drawCircle with radial gradient), and disabled state in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/LaunchButton.kt`
- [ ] T020 [P] [US1] Create AirportSelectionSheet composable (bottom sheet) for origin/destination selection in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/AirportSelectionSheet.kt`
- [ ] T021 [P] [US1] Create DateSelectionSheet composable for date picker with formatted display in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/DateSelectionSheet.kt`
- [ ] T022 [P] [US1] Create PassengerSelectionSheet composable for passenger count selection (1-9) in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/PassengerSelectionSheet.kt`
- [ ] T023 [US1] Update SearchScreenModel to use VelocitySearchState and handle field selection events in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/search/SearchScreenModel.kt`
- [ ] T024 [US1] Implement destination filtering logic in SearchScreenModel - only show valid routes from selected origin in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/search/SearchScreenModel.kt`
- [ ] T025 [US1] Create VelocitySearchScreen composable with VelocityTheme wrapper, SentenceBuilder, and LaunchButton in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/search/VelocitySearchScreen.kt`
- [ ] T026 [US1] Wire VelocitySearchScreen to SearchScreenModel for state and event handling in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/search/VelocitySearchScreen.kt`
- [ ] T027 [US1] Update SearchScreen.kt to use VelocitySearchScreen implementation in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/search/SearchScreen.kt`

**Checkpoint**: User Story 1 complete - sentence builder search is fully functional with tappable fields, selection sheets, and launch button

---

## Phase 4: User Story 2 - Glassmorphic Flight Results Display (Priority: P1)

**Goal**: Implement the slide-up results overlay with glassmorphic flight cards showing times, route, and price

**Independent Test**: After a search, the results overlay slides up from the bottom showing flight cards with times, route line, duration, and price.

### Implementation for User Story 2

- [ ] T028 [P] [US2] Create FlightRouteVisual composable with departure/arrival codes and animated route line with accent dot in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/FlightRouteVisual.kt`
- [ ] T029 [P] [US2] Create VelocityFlightCardView composable using GlassCard with times, route visual, duration, and price in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/VelocityFlightCardView.kt`
- [ ] T030 [P] [US2] Create ResultsHeader composable showing route (RUH → DXB) and formatted date with close button in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/ResultsHeader.kt`
- [ ] T031 [US2] Create VelocityResultsOverlay composable with AnimatedVisibility (slideInVertically/slideOutVertically) in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/results/VelocityResultsOverlay.kt`
- [ ] T032 [US2] Implement LazyVerticalGrid with GridCells.Adaptive(300.dp) for responsive flight card layout in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/results/VelocityResultsOverlay.kt`
- [ ] T033 [US2] Create FlightDto to VelocityFlightCard mapper function in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/results/FlightMapper.kt`
- [ ] T034 [US2] Update ResultsScreenModel to use VelocityResultsState and handle overlay visibility in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/results/ResultsScreenModel.kt`
- [ ] T035 [US2] Wire VelocityResultsOverlay to ResultsScreenModel for state and events in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/results/VelocityResultsOverlay.kt`
- [ ] T036 [US2] Integrate VelocityResultsOverlay into VelocitySearchScreen as overlay layer in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/search/VelocitySearchScreen.kt`

**Checkpoint**: User Stories 1 AND 2 complete - full search and results flow working with glassmorphic cards

---

## Phase 5: User Story 3 - Expandable Fare Family Selection (Priority: P2)

**Goal**: Implement expandable flight cards with fare grid showing Fly, Fly+, FlyMax options

**Independent Test**: Tapping a flight card expands it to reveal three fare option tiles; tapping another card collapses the previous one.

### Implementation for User Story 3

- [ ] T037 [P] [US3] Create FareTile composable showing fare name and price with accent highlight on selection in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/FareTile.kt`
- [ ] T038 [P] [US3] Create FareGrid composable with 3-column Row layout for Fly, Fly+, FlyMax tiles in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/FareGrid.kt`
- [ ] T039 [US3] Add animateContentSize modifier to VelocityFlightCardView for smooth expand/collapse in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/VelocityFlightCardView.kt`
- [ ] T040 [US3] Integrate FareGrid into VelocityFlightCardView, shown when isExpanded is true in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/VelocityFlightCardView.kt`
- [ ] T041 [US3] Add card expansion logic to ResultsScreenModel - only one card expanded at a time in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/results/ResultsScreenModel.kt`
- [ ] T042 [US3] Add fare selection handling to ResultsScreenModel in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/results/ResultsScreenModel.kt`

**Checkpoint**: User Stories 1, 2, AND 3 complete - search, results, and fare selection all working

---

## Phase 6: User Story 4 - Dynamic Destination Backgrounds (Priority: P2)

**Goal**: Implement destination-specific background images that fade in/out based on selection

**Independent Test**: Selecting Dubai as destination fades in a Dubai skyline background; selecting Cairo shows pyramids/Egypt imagery.

### Implementation for User Story 4

- [ ] T043 [P] [US4] Create DestinationBackground composable with AsyncImage/painterResource and crossfade animation in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/DestinationBackground.kt`
- [ ] T044 [P] [US4] Add graphicsLayer modifier to DestinationBackground for 40% brightness reduction in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/DestinationBackground.kt`
- [ ] T045 [US4] Add AnimatedVisibility wrapper with fadeIn/fadeOut for background transitions in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/DestinationBackground.kt`
- [ ] T046 [US4] Add destinationBackground state updates to SearchScreenModel when destination changes in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/search/SearchScreenModel.kt`
- [ ] T047 [US4] Integrate DestinationBackground into VelocitySearchScreen as bottom layer in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/search/VelocitySearchScreen.kt`

**Checkpoint**: User Stories 1-4 complete - full visual experience with dynamic backgrounds

---

## Phase 7: User Story 5 - RTL/Arabic Language Support (Priority: P3)

**Goal**: Implement RTL layout and Arabic typography with language toggle

**Independent Test**: Tapping the language toggle switches all text to Arabic, reverses layout direction, and applies Arabic font.

### Implementation for User Story 5

- [ ] T048 [P] [US5] Create ArabicTypography with Noto Kufi Arabic font family definitions matching English weights in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/theme/VelocityTypography.kt`
- [ ] T049 [P] [US5] Create LanguageToggle composable button for EN/AR switching in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/LanguageToggle.kt`
- [ ] T050 [US5] Update VelocityTheme to accept isRtl parameter and provide LocalLayoutDirection accordingly in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/theme/VelocityTheme.kt`
- [ ] T051 [US5] Update VelocityTheme to switch between SpaceGrotesk and NotoKufiArabic based on language in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/theme/VelocityTheme.kt`
- [ ] T052 [US5] Integrate LanguageToggle into VelocitySearchScreen header area in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/search/VelocitySearchScreen.kt`
- [ ] T053 [US5] Wire language toggle to LocalizationManager for state persistence in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/search/VelocitySearchScreen.kt`
- [ ] T054 [US5] Verify all SentenceBuilder text uses localized strings from Strings.kt in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/SentenceBuilder.kt`

**Checkpoint**: All user stories complete - full Velocity UI with RTL support

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Error handling, edge cases, and final refinements

- [ ] T055 [P] Create EmptyResultsState composable for no flights available message in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/EmptyResultsState.kt`
- [ ] T056 [P] Create ErrorResultsState composable with retry button for network failures in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/ErrorResultsState.kt`
- [ ] T057 [P] Create LoadingResultsState composable with accent-colored progress indicator in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/LoadingResultsState.kt`
- [ ] T058 Integrate EmptyResultsState, ErrorResultsState, LoadingResultsState into VelocityResultsOverlay in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/screens/results/VelocityResultsOverlay.kt`
- [ ] T059 Add fallback gradient background when destination images fail to load in `apps-kmp/src/commonMain/kotlin/com/flyadeal/app/ui/components/velocity/DestinationBackground.kt`
- [ ] T060 Verify 60fps animations using Android Studio profiler
- [ ] T061 Test on Android, iOS simulator, and Web (Wasm) per quickstart.md checklist
- [ ] T062 Visual comparison with design.html to verify 95%+ fidelity

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - US1 and US2 are both P1 and can proceed in parallel
  - US3 depends on US2 (extends flight cards)
  - US4 can proceed independently after Foundational
  - US5 can proceed independently after Foundational
- **Polish (Phase 8)**: Depends on US1 + US2 minimum, ideally all stories

### User Story Dependencies

```
Foundational (Phase 2)
        │
        ├──► US1 (P1) Search Interface
        │         │
        │         └──► US2 (P1) Results Display ──► US3 (P2) Fare Selection
        │
        ├──► US4 (P2) Dynamic Backgrounds (independent)
        │
        └──► US5 (P3) RTL Support (independent)
```

### Within Each User Story

- Models/state before components
- Components before screen integration
- Screen integration before wiring to ScreenModel

### Parallel Opportunities

**Phase 1 (all parallel)**:
- T001, T002, T003 - all font/image downloads

**Phase 2 (partial parallel)**:
- T007, T008 - glassmorphism components
- T009, T010, T011, T012, T013, T014 - all state/model definitions
- T015, T016 - string resources

**Phase 3 - US1 (partial parallel)**:
- T017, T018, T019, T020, T021, T022 - all independent components

**Phase 4 - US2 (partial parallel)**:
- T028, T029, T030 - all independent components

**Phase 5 - US3**:
- T037, T038 - fare components (parallel)

**Phase 6 - US4**:
- T043, T044 - background component (same file, sequential)

**Phase 7 - US5 (partial parallel)**:
- T048, T049 - typography and toggle (parallel)

**Phase 8 (all parallel)**:
- T055, T056, T057 - all state composables

---

## Parallel Example: User Story 1

```bash
# Launch all independent components for User Story 1 together:
Task: T017 "Create SentenceBuilder composable"
Task: T018 "Create MagicInputField composable"
Task: T019 "Create LaunchButton composable"
Task: T020 "Create AirportSelectionSheet composable"
Task: T021 "Create DateSelectionSheet composable"
Task: T022 "Create PassengerSelectionSheet composable"

# Then sequentially:
Task: T023 "Update SearchScreenModel"
Task: T024 "Implement destination filtering"
Task: T025 "Create VelocitySearchScreen"
Task: T026 "Wire VelocitySearchScreen to SearchScreenModel"
Task: T027 "Update SearchScreen.kt"
```

---

## Implementation Strategy

### MVP First (User Stories 1 + 2)

1. Complete Phase 1: Setup (fonts, images)
2. Complete Phase 2: Foundational (theme, state models)
3. Complete Phase 3: User Story 1 (search interface)
4. Complete Phase 4: User Story 2 (results display)
5. **STOP and VALIDATE**: Test end-to-end search flow
6. Deploy/demo if ready - core Velocity experience complete

### Incremental Delivery

1. Setup + Foundational → Design system ready
2. Add US1 (Search) → Test search interface → Demo
3. Add US2 (Results) → Test full flow → Demo (MVP!)
4. Add US3 (Fares) → Test expansion → Demo
5. Add US4 (Backgrounds) → Test transitions → Demo
6. Add US5 (RTL) → Test Arabic → Demo
7. Polish phase → Final release

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (Search)
   - Developer B: User Story 4 (Backgrounds)
   - Developer C: User Story 5 (RTL)
3. After US1 complete:
   - Developer A: User Story 2 (Results)
4. After US2 complete:
   - Developer A: User Story 3 (Fares)
5. Stories integrate incrementally

---

## Summary

| Metric | Value |
|--------|-------|
| Total Tasks | 62 |
| Setup Tasks | 3 |
| Foundational Tasks | 13 |
| US1 Tasks | 11 |
| US2 Tasks | 9 |
| US3 Tasks | 6 |
| US4 Tasks | 5 |
| US5 Tasks | 7 |
| Polish Tasks | 8 |
| Parallel Opportunities | 35 tasks (56%) |

**MVP Scope**: Phases 1-4 (Setup + Foundational + US1 + US2) = 36 tasks
**Full Feature**: All 62 tasks

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Refer to quickstart.md for testing checklist
- Refer to research.md for implementation patterns
- Refer to data-model.md for state structure
