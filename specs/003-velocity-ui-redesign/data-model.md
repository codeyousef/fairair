# Data Model: Velocity UI Redesign

**Feature**: 003-velocity-ui-redesign
**Date**: 2025-11-29

## Overview

This document defines the UI state models and entities required for the Velocity UI redesign. Since this is a UI-only change, backend models remain unchanged. Focus is on frontend state management and display models.

## UI State Models

### VelocitySearchState

Manages the state for the natural language sentence-builder search interface.

| Field | Type | Description |
|-------|------|-------------|
| selectedOrigin | Station? | Currently selected origin airport |
| selectedDestination | Station? | Currently selected destination airport |
| departureDate | LocalDate | Selected departure date |
| passengerCount | Int | Number of adult passengers (1-9) |
| availableOrigins | List<Station> | Airports available as origins |
| availableDestinations | List<Station> | Valid destinations for selected origin |
| activeField | SearchField? | Currently active/focused input field |
| isSearchEnabled | Boolean | Whether search button should be enabled |
| destinationBackground | DestinationTheme? | Active background for selected destination |

**Computed Properties**:
- `isSearchEnabled` = origin != null && destination != null
- `formattedDate` = departureDate formatted as "Dec 01"
- `passengerLabel` = "$passengerCount Adult" or "$passengerCount Adults"

**Validation Rules**:
- Origin and destination cannot be the same
- Passenger count must be between 1 and 9
- Departure date cannot be in the past

---

### VelocityResultsState

Manages the state for the flight results overlay.

| Field | Type | Description |
|-------|------|-------------|
| isVisible | Boolean | Whether results overlay is shown |
| flights | List<VelocityFlightCard> | Flight results to display |
| expandedFlightId | String? | ID of currently expanded flight card |
| selectedFlight | VelocityFlightCard? | Currently selected flight |
| selectedFare | FareFamily? | Currently selected fare within flight |
| routeDisplay | String | Formatted route (e.g., "RUH → DXB") |
| dateDisplay | String | Formatted date (e.g., "MON, 01 DEC") |
| isLoading | Boolean | Whether results are loading |
| error | String? | Error message if search failed |

**State Transitions**:
- HIDDEN → LOADING → VISIBLE (success) or ERROR (failure)
- Expanding a card: set expandedFlightId, collapse previous
- Selecting a fare: set selectedFlight and selectedFare

---

### VelocityFlightCard

Display model for a single flight result card.

| Field | Type | Description |
|-------|------|-------------|
| id | String | Unique flight identifier (flight number) |
| departureTime | String | Formatted departure time (e.g., "08:00") |
| arrivalTime | String | Formatted arrival time (e.g., "09:40") |
| originCode | String | 3-letter origin airport code |
| destinationCode | String | 3-letter destination airport code |
| durationMinutes | Int | Flight duration in minutes |
| durationFormatted | String | Formatted duration (e.g., "1h 40m") |
| lowestPrice | Money | Lowest fare price for display |
| fareFamilies | List<FareFamily> | Available fare options (Fly, Fly+, FlyMax) |
| isExpanded | Boolean | Whether card is currently expanded |
| isSelected | Boolean | Whether this flight is selected |

**Computed Properties**:
- `durationFormatted` = "${durationMinutes / 60}h ${durationMinutes % 60}m"
- `lowestPrice` = fareFamilies.minBy { it.price }

---

### FareFamily

Display model for a fare option within a flight card.

| Field | Type | Description |
|-------|------|-------------|
| code | FareFamilyCode | Enum: FLY, FLY_PLUS, FLY_MAX |
| displayName | String | Human-readable name (e.g., "Fly+") |
| price | Money | Price for this fare |
| isSelected | Boolean | Whether this fare is currently selected |

**FareFamilyCode Enum**:
- FLY - Basic fare
- FLY_PLUS - Enhanced fare with extras
- FLY_MAX - Premium fare with full benefits

---

### DestinationTheme

Configuration for destination-specific background imagery.

| Field | Type | Description |
|-------|------|-------------|
| destinationCode | String | 3-letter airport code |
| backgroundImageUrl | String | URL or resource path for background image |
| isActive | Boolean | Whether this background is currently shown |

**Predefined Destinations**:
| Code | Image Description |
|------|-------------------|
| JED | Jeddah cityscape/coastline |
| DXB | Dubai skyline with Burj Khalifa |
| CAI | Cairo with pyramids |
| RUH | Riyadh skyline |
| DMM | Dammam coastal view |

---

### SearchField

Enum representing which field in the sentence builder is active.

| Value | Description |
|-------|-------------|
| ORIGIN | Origin airport selector |
| DESTINATION | Destination airport selector |
| DATE | Date picker |
| PASSENGERS | Passenger count selector |

---

## Theme/Style Models

### VelocityColors

Color definitions for the Velocity design system.

| Property | Value | Usage |
|----------|-------|-------|
| backgroundDeep | #120521 | Primary app background |
| accent | #CCFF00 | Highlights, CTAs, interactive elements |
| glassBg | rgba(255,255,255,0.1) | Card backgrounds |
| glassHover | rgba(255,255,255,0.15) | Card hover state |
| glassBorder | rgba(255,255,255,0.1) | Card borders |
| textMain | #FFFFFF | Primary text |
| textMuted | rgba(255,255,255,0.6) | Secondary text |
| neonGlow | rgba(204,255,0,0.3) | Button glow effect |

---

### VelocityTypography

Typography definitions for the Velocity design.

| Style | Font | Weight | Size | Usage |
|-------|------|--------|------|-------|
| heroTitle | Space Grotesk | 300 | 64sp | Main headline |
| sentenceBuilder | Space Grotesk | 300 | 40sp | Search sentence |
| magicInput | Space Grotesk | 700 | 40sp | Inline selectable fields |
| flightPath | Space Grotesk | 700 | 48sp | Route display in results |
| timeBig | Space Grotesk | 700 | 32sp | Departure/arrival times |
| priceDisplay | Space Grotesk | 700 | 28sp | Price in cards |
| labelSmall | Space Grotesk | 700 | 10sp | Fare family labels |

**RTL Typography** (Arabic):
| Style | Font | Weight | Size |
|-------|------|--------|------|
| heroTitle | Noto Kufi Arabic | 300 | 48sp |
| sentenceBuilder | Noto Kufi Arabic | 300 | 32sp |
| (others) | Noto Kufi Arabic | (same weights) | (adjusted sizes) |

---

### VelocityAnimations

Animation specifications for transitions.

| Animation | Duration | Easing | Description |
|-----------|----------|--------|-------------|
| overlaySlide | 600ms | cubic-bezier(0.7, 0, 0.3, 1) | Results overlay enter/exit |
| cardExpand | 400ms | ease-out | Card expand/collapse |
| backgroundFade | 1000ms | ease | Destination background crossfade |
| buttonScale | 300ms | ease | Launch button hover/press |
| shimmer | 500ms | linear | Card hover shimmer effect |

---

## Relationship Diagram

```
VelocitySearchState
    ├── selectedOrigin ──────► Station (from API)
    ├── selectedDestination ─► Station (from API)
    ├── destinationBackground ► DestinationTheme
    └── [triggers] ──────────► VelocityResultsState

VelocityResultsState
    ├── flights ─────────────► List<VelocityFlightCard>
    │                              └── fareFamilies ──► List<FareFamily>
    ├── selectedFlight ──────► VelocityFlightCard
    └── selectedFare ────────► FareFamily
```

---

## Mapping from Backend DTOs

The UI models map from existing backend DTOs:

| Backend DTO | UI Model | Transformation |
|-------------|----------|----------------|
| StationDto | Station | Direct mapping |
| FlightDto | VelocityFlightCard | Extract times, format duration |
| FareDto | FareFamily | Map fareFamily to FareFamilyCode |
| RouteMapDto | availableDestinations | Filter by selected origin |

No backend API changes required. All transformations happen in the ScreenModel layer.
