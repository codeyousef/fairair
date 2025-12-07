# FareAir AI Agent Demo - Technical Specification

## 1. Project Overview

Goal: Demonstrate a "Voice-First" airline experience for FareAir where users can search, book, and manage flights using natural language (English & Saudi Arabic).

Key Value: Accelerates complex flows (like changing a seat or splitting a booking) from 10+ clicks to 2 voice commands.

### Technology Stack

- **Frontend:** Kotlin Multiplatform (KMP) - Android & iOS (Compose Multiplatform).
    
- **Backend:** Spring Boot (Webflux) - Middleware & Orchestrator.
    
- **AI Model (Unified):** **Meta Llama 3.1 70B Instruct**.
    
    - _Availability:_ Available on **Google Vertex AI** (for Demo) and **AWS Bedrock** (for Prod).
        
    - _Benefit:_ Zero prompt rewriting when migrating. Excellent Arabic support. Low cost.
        
- **Mock System:** In-memory "Mock Navitaire" service to simulate FareAir airline logic.
    

## 2. Architecture: Hexagonal (Ports & Adapters)

**Critical Rule:** The Core Domain (Mock Navitaire) and Frontend (KMP) must NEVER import Vertex or Bedrock libraries directly. All AI interaction is behind the `GenAiProvider` interface.

### The Interface Contract

```
interface GenAiProvider {
    suspend fun generateResponse(
        session: ChatSession, 
        userMessage: String, 
        tools: List<ToolDefinition>
    ): AiResponse
}
```

## 3. Frontend Specification (KMP)

### 3.1 Visual Identity

- **Theme Strategy:** Inherit strictly from the existing FareAir application theme configuration.
    
- **Colors:** Use semantic references (e.g., `MaterialTheme.colorScheme.primary`).
    
- **Layout Direction:** Dynamic (LTR for English, RTL for Arabic).
    

### 3.2 Core Components

1. **The "Faris" Orb (AI Assistant):**
    
    - Floating Action Button (FAB) overlay.
        
    - Action: Opens half-height BottomSheet.
        
2. **Generative UI Widgets (Polymorphic List):**
    
    - `TextBubble`: Markdown support.
        
    - `FlightCarousel`: Horizontal scroll of flight options.
        
    - `SeatMapWidget`: Mini interactive seat map.
        
    - `BoardingPassCard`: QR Code rendering.
        
    - `ComparisonCard`: "Old Flight vs New Flight" with price difference.
        

## 4. Backend Specification (Spring Webflux)

### 4.1 Data Models (DTOs)

**`AiResponse` (Returned to Client)**

```
data class AiResponse(
    val text: String,        // "I found 3 flights..."
    val uiType: String?,     // "FLIGHT_LIST", "SEAT_MAP", "BOARDING_PASS"
    val uiData: String?      // JSON String of the payload for the UI
)
```

### 4.2 Mock Navitaire Service (Crucial Logic)

**Feature: Split PNR (Partial Cancellation)**

- **Concept:** To cancel ONE person in a group, you must split them into a new PNR, then cancel that PNR.
    
- **Mock Logic:**
    
    1. Find Booking `ABC123`.
        
    2. Find Passenger `Sarah` in list.
        
    3. Remove `Sarah` from `ABC123`.
        
    4. Create new (cancelled) Booking `XYZ999` for `Sarah`.
        
    5. Return success message.
        

## 5. Unified Configuration (Llama 3.1)

Since we are using Llama 3.1 on both clouds, the schemas are identical.

### 5.1 Tool Definitions (JSON Schema)

**1. `search_flights`**

```
{
  "name": "search_flights",
  "description": "Finds available flights based on origin, destination, and date.",
  "parameters": {
    "type": "object",
    "properties": {
      "origin": {"type": "string", "description": "IATA Code (e.g. RUH)"},
      "destination": {"type": "string", "description": "IATA Code (e.g. JED)"},
      "date": {"type": "string", "description": "YYYY-MM-DD"}
    },
    "required": ["origin", "destination"]
  }
}
```

**2. `cancel_specific_passenger`**

```
{
  "name": "cancel_specific_passenger",
  "description": "Cancels ONE specific person from a booking, leaving others active.",
  "parameters": {
    "type": "object",
    "properties": {
      "pnr": {"type": "string"},
      "passenger_name": {"type": "string", "description": "First name of passenger to remove"}
    },
    "required": ["pnr", "passenger_name"]
  }
}
```

### 5.2 System Prompt (Bilingual Persona)

**Role:** "Faris", FareAir's intelligent assistant.

**Instructions:**

1. **Language:** Detect user language.
    
    - If **English**: Be concise, professional.
        
    - If **Arabic**: Use **Saudi White Dialect** (Khaleeji). Use terms like "Abshir" (Sure), "Halla" (Welcome), "Sim" (Yes/Ok). **DO NOT use formal MSA (Fusha).**
        
2. **Tool Use:** ALWAYS use English values for tool arguments (e.g., City Codes `RUH`, `JED`).
    
3. **Behavior:**
    
    - If user says "Change seat", ask "Aisle or Window?".
        
    - If user says "Cancel Sarah", ask for confirmation first.
        

## 6. Migration Guide (Vertex -> Bedrock)

Because we chose Llama 3.1, the migration is purely a **Client Swap**.

### Phase 1: Vertex AI (Demo)

- **Library:** `spring-ai-openai` (Using OpenAI compatibility layer for Vertex MaaS).
    
- **Service:** Llama 3.1 API Service (Vertex AI Model Garden).
    
- **Configuration:**
    
    - `ENDPOINT`: `us-central1-aiplatform.googleapis.com`
        
    - `REGION`: `us-central1`
        
    - `PROJECT_ID`: `"YOUR_PROJECT_ID"`
        
- **Authentication:** Google Service Account / Application Default Credentials.
    

### Phase 2: AWS Bedrock (Production)

- **Library:** `aws-java-sdk-bedrockruntime` (or `spring-ai-bedrock`).
    
- **Endpoint:** `meta.llama3-1-70b-instruct-v1:0`.
    
- **Authentication:** AWS IAM Role.
    

**No Prompt Engineering changes required.**

## 7. Integration Contract (API)

**Endpoint:** `POST /api/chat/v1/message`

**Response (Standard):**

```
{
  "text": "Abshir! Sarah is currently in 12B. I can move her to 12F (Window) or 12C (Aisle).",
  "uiType": "SEAT_MAP",
  "uiData": {
    "pnr": "ABC123",
    "highlightedRow": 12,
    "availableSeats": ["12F", "12C"]
  }
}
```