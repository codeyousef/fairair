# Feature Specification: Velocity UI Redesign

**Feature Branch**: `003-velocity-ui-redesign`
**Created**: 2025-11-29
**Status**: Draft
**Input**: User description: "Change the design of the apps to be a direct translation of design.html - a modern, glassmorphic velocity experience UI"

## Overview

Transform the existing flyadeal mobile/web application UI to match the "Velocity Experience" design language defined in design.html. This is a visual and interaction redesign focusing on:

- **Deep purple/black background** with gradient overlays
- **Glassmorphism** aesthetic (frosted glass cards, blur effects)
- **Natural language sentence builder** for flight search (conversational UI)
- **Neon lime accent color** (#ccff00) for highlights and CTAs
- **Dynamic destination backgrounds** that change based on user selection
- **Holographic flight result cards** with expandable fare options
- **Full RTL/Arabic support** with Noto Kufi Arabic font

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Natural Language Flight Search (Priority: P1)

As a traveler, I want to search for flights using a conversational sentence-builder interface ("I want to fly from [Riyadh] to [Dubai] departing on [Dec 01] with [1 Adult]") so that booking feels intuitive and human rather than filling out traditional forms.

**Why this priority**: This is the core interaction paradigm shift from traditional form-based search to conversational UI. It's the first thing users see and interact with.

**Independent Test**: User can complete a flight search by tapping on highlighted words in a sentence, selecting values from dropdowns, and launching search - the search request is sent to the backend.

**Acceptance Scenarios**:

1. **Given** the user is on the home screen, **When** they view the search interface, **Then** they see a sentence: "I want to fly from [Origin] to [Destination] departing on [Date] with [Passengers]." with tappable highlighted words
2. **Given** the user taps on the origin field, **When** a selection sheet appears, **Then** they can choose from available departure cities (Riyadh, Jeddah, Dammam)
3. **Given** the user selects an origin city, **When** they tap the destination field, **Then** only valid destinations for that origin are shown
4. **Given** all search fields are populated, **When** the user taps the circular launch button, **Then** a flight search is initiated and results overlay appears
5. **Given** the destination field has not been selected, **When** the user views the launch button, **Then** it appears disabled (grayed out)

---

### User Story 2 - Glassmorphic Flight Results Display (Priority: P1)

As a traveler, I want to view flight search results in visually stunning glass-effect cards that show departure/arrival times, route visualization, duration, and starting price so I can quickly scan and compare options.

**Why this priority**: This is the second core screen users interact with after search. Visual appeal and usability directly impact conversion.

**Independent Test**: After a search, the results overlay slides up from the bottom showing flight cards with times, route line, duration, and price.

**Acceptance Scenarios**:

1. **Given** a search has been initiated, **When** results are ready, **Then** a full-screen overlay slides up from the bottom with backdrop blur
2. **Given** the results overlay is visible, **When** the user views a flight card, **Then** they see: departure time (large), origin code, animated route line with accent dot, arrival time (large), destination code, duration label, and lowest fare price
3. **Given** the user views multiple cards, **When** they compare flights, **Then** each card has identical layout but different data, using glassmorphic styling (semi-transparent background, subtle border, blur)
4. **Given** the user wants to close results, **When** they tap the "Close" button, **Then** the overlay slides back down revealing the search screen

---

### User Story 3 - Expandable Fare Family Selection (Priority: P2)

As a traveler, I want to tap a flight card to expand it and see all fare options (Fly, Fly+, FlyMax) with their prices so I can choose the fare that matches my needs.

**Why this priority**: Fare selection is essential for booking but secondary to finding flights. Users must first see results before selecting fares.

**Independent Test**: Tapping a flight card expands it to reveal three fare option tiles; tapping another card collapses the previous one.

**Acceptance Scenarios**:

1. **Given** the user views flight results, **When** they tap a flight card, **Then** the card expands to reveal a 3-column fare grid (Fly, Fly+, FlyMax)
2. **Given** a card is expanded, **When** the user views fare options, **Then** each shows: fare name (uppercase label), price
3. **Given** a card is expanded, **When** the user taps a different card, **Then** the previously expanded card collapses and the new one expands
4. **Given** the user hovers/taps a fare option, **When** interaction occurs, **Then** the fare tile highlights with the accent color

---

### User Story 4 - Dynamic Destination Backgrounds (Priority: P2)

As a traveler, I want the background to change to a beautiful destination image when I select where I'm going so the app feels immersive and inspiring.

**Why this priority**: Enhances emotional engagement but is not critical to core functionality.

**Independent Test**: Selecting Dubai as destination fades in a Dubai skyline background; selecting Cairo shows pyramids/Egypt imagery.

**Acceptance Scenarios**:

1. **Given** the user is on the search screen with default gradient background, **When** they select a destination, **Then** a destination-specific background image fades in (opacity transition)
2. **Given** a destination background is shown, **When** the user changes to a different destination, **Then** the old image fades out and new one fades in
3. **Given** a destination background is shown, **When** the image is visible, **Then** it has reduced brightness (40%) and increased saturation for visual depth
4. **Given** no destination is selected, **When** the user views the screen, **Then** only the purple gradient background is visible

---

### User Story 5 - RTL/Arabic Language Support (Priority: P3)

As an Arabic-speaking traveler, I want to switch the app to Arabic with proper right-to-left layout and Arabic typography so I can use the app in my preferred language.

**Why this priority**: Important for Saudi market but can be added after core UI is functional in English.

**Independent Test**: Tapping the language toggle switches all text to Arabic, reverses layout direction, and applies Arabic font.

**Acceptance Scenarios**:

1. **Given** the user is viewing the app in English, **When** they tap the language toggle button, **Then** the entire UI switches to Arabic with RTL layout
2. **Given** the app is in Arabic mode, **When** the user views the search sentence, **Then** it reads naturally in Arabic: "أبي أسافر من [الرياض] إلى [دبي] بتاريخ [1 ديسمبر] لعدد [1 بالغ]"
3. **Given** the app is in Arabic mode, **When** the user views any text, **Then** Noto Kufi Arabic font is used
4. **Given** the app is in Arabic mode, **When** the user taps the language toggle, **Then** the UI switches back to English LTR

---

### Edge Cases

- What happens when no flights are available for the selected route/date? Display an empty state message within the results overlay
- What happens when the network request fails? Show an error state with retry option
- What happens on very small screens? Sentence builder should wrap gracefully; font sizes reduce proportionally
- What happens when background images fail to load? Fallback to gradient-only background

## Requirements *(mandatory)*

### Functional Requirements

#### Visual Design System

- **FR-001**: App MUST use deep purple/black (#120521) as primary background color
- **FR-002**: App MUST use neon lime (#ccff00) as accent color for highlights, CTAs, and interactive elements
- **FR-003**: All cards MUST use glassmorphism style: semi-transparent white background (rgba 255,255,255,0.1), subtle white border, backdrop blur effect
- **FR-004**: Typography MUST use Space Grotesk font family for English, Noto Kufi Arabic for Arabic
- **FR-005**: Interactive elements MUST have smooth transitions (0.3-0.4s with cubic-bezier easing)

#### Search Interface

- **FR-006**: Search interface MUST present as a natural language sentence with tappable/selectable inline fields
- **FR-007**: Origin field MUST show available departure cities (Riyadh, Jeddah, Dammam initially)
- **FR-008**: Destination field MUST dynamically filter to show only valid routes from selected origin
- **FR-009**: Date field MUST use native date picker with formatted display (e.g., "Dec 01")
- **FR-010**: Passenger field MUST allow selection of 1-2 adults initially
- **FR-011**: Launch button MUST be circular (80px), accent-colored, with arrow icon
- **FR-012**: Launch button MUST be disabled (grayed) until destination is selected

#### Results Display

- **FR-013**: Results MUST appear as full-screen overlay that slides up from bottom
- **FR-014**: Results header MUST show route (e.g., "RUH → DXB") and formatted date
- **FR-015**: Flight cards MUST display in responsive grid (auto-fill, min 300px)
- **FR-016**: Each flight card MUST show: departure time, origin code, visual route line with accent dot, arrival time, destination code, duration, lowest price
- **FR-017**: Flight cards MUST be tappable to expand and show fare options
- **FR-018**: Only one card can be expanded at a time

#### Fare Selection

- **FR-019**: Expanded cards MUST show 3-column fare grid: Fly, Fly+, FlyMax
- **FR-020**: Each fare tile MUST show fare name and price
- **FR-021**: Fare tiles MUST highlight on hover/tap with accent color

#### Dynamic Backgrounds

- **FR-022**: App MUST support destination-specific background images
- **FR-023**: Background images MUST have reduced brightness (40%) and fade in/out on selection
- **FR-024**: Default state MUST show purple radial gradient background only

#### Localization

- **FR-025**: App MUST support English (LTR) and Arabic (RTL) languages
- **FR-026**: Language toggle MUST be accessible from header
- **FR-027**: All UI text MUST be translatable
- **FR-028**: Layout MUST automatically flip for RTL languages

### Key Entities

- **SearchCriteria**: Origin airport, destination airport, departure date, passenger count
- **FlightCard**: Flight number, departure time, arrival time, origin code, destination code, duration, fare families
- **FareFamily**: Code (FLY, FLY_PLUS, FLY_MAX), display name, price
- **DestinationTheme**: Destination code, background image URL, active state

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can complete a flight search using the sentence-builder interface in under 30 seconds
- **SC-002**: Flight results overlay appears within 1 second of tapping the launch button (excluding network latency)
- **SC-003**: Background transitions and card animations complete smoothly at 60fps on target devices
- **SC-004**: All interactive elements provide visual feedback within 100ms of user interaction
- **SC-005**: Language switching completes instantly with no visible layout jumps or text overflow
- **SC-006**: App visual design matches design.html reference at 95%+ fidelity as verified by visual comparison
- **SC-007**: All user stories pass acceptance testing on Android, iOS, and Web (Wasm) platforms

## Assumptions

- The existing backend API (Spring Boot) returns data in a format compatible with the new UI (flight search, routes, stations endpoints remain unchanged)
- Destination background images will be bundled with the app or loaded from a CDN
- The 3 fare families (Fly, Fly+, FlyMax) are the standard offerings
- Initial implementation focuses on one-way flights; round-trip can be added later
- Passenger types beyond adult (children, infants) will use the same UI pattern when added
