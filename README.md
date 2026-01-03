# ✈️ FairAir

### Reimagining Air Travel as an AI-First Experience

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.7.1-4285F4?style=for-the-badge&logo=jetpack-compose&logoColor=white)
![Google Cloud](https://img.shields.io/badge/Google_Cloud-Vertex_AI-4285F4?style=for-the-badge&logo=google-cloud&logoColor=white)

---

**FairAir** is a next-generation airline booking platform built to demonstrate the power of **Kotlin Multiplatform** and **Generative AI**. It moves beyond traditional static booking flows, offering a conversational, intelligent, and adaptive user experience.

## 🌟 Key Features

### 🤖 AI-First Booking
Forget complex forms. Just say *"I want to fly to Dubai next weekend with my wife"* and let FairAir handle the rest.
- **Voice-Native**: Real-time voice conversation powered by **Dialogflow CX** and **Google Cloud Speech**.
- **Smart Extraction**: **Vertex AI (Llama 3.1)** understands intent, dates, and destinations from natural language.
- **Context Aware**: Remembers your preferences and conversation history.

### 📱 True Multiplatform
One codebase, running natively everywhere.
- **Android**: Native performance and Material 3 design.
- **iOS**: Native UI via Compose Multiplatform.
- **Web (Wasm)**: High-performance web assembly application.

### ⚡ Reactive Backend
Built for scale and speed.
- **Spring Boot WebFlux**: Fully non-blocking I/O.
- **Coroutines**: Structured concurrency from database to UI.
- **Hexagonal Architecture**: Clean separation of concerns.

---

## 🏗️ Architecture

The project is organized into a clean monorepo structure:

| Module | Description | Tech Stack |
|--------|-------------|------------|
| **`:apps-kmp`** | Frontend applications (Android, iOS, Web) | Compose Multiplatform, Voyager, Koin |
| **`:backend-spring`** | BFF (Backend for Frontend) API | Spring Boot, WebFlux, R2DBC, Vertex AI |
| **`:shared-contract`** | Shared DTOs and API definitions | Kotlin Serialization, KMP |

```mermaid
graph LR
    User((User)) --> Client[Apps KMP\n(Android/iOS/Web)]
    Client --> BFF[Backend Spring\n(WebFlux)]
    BFF --> AI[Vertex AI & Dialogflow]
    BFF --> DB[(Database)]
    BFF --> Navitaire[Airline Core System]
```

---

## 🚀 Getting Started

### Prerequisites
- JDK 17+
- Docker (for containerization)
- Google Cloud Project with Vertex AI enabled

### Environment Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/codeyousef/fairair.git
   ```
2. Configure GCP credentials in `backend-spring/src/main/resources/application-local.yml`.

### Running the Project

**Backend (Spring Boot):**
```bash
./gradlew :backend-spring:bootRun
```
*Server starts at `http://localhost:8080`*

**Web App (Compose Wasm):**
```bash
./gradlew :apps-kmp:wasmJsBrowserRun
```
*Client starts at `http://localhost:8081`*

**Android App:**
```bash
./gradlew :apps-kmp:installDebug
```

---

## 🧠 AI Configuration

FairAir uses a dual-AI approach:
1. **Dialogflow CX**: Handles real-time voice streaming and conversation flow.
2. **Vertex AI (Llama 3.1)**: Handles complex entity extraction and reasoning.

To switch AI providers or models, check `FairairProperties.kt` and `application.yml`.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Built with ❤️ by Yousef*
