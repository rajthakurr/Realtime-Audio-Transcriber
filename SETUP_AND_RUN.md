# Audio Transcription + Circular Equalizer - Setup & Run Guide

## 🎯 Project Overview

This project consists of two main components:

1. **Frontend**: Circular Audio Equalizer UI with real-time visualization
2. **Backend**: Spring Boot service for real-time audio streaming and transcription using Gemini API

## 📋 Prerequisites

Ensure you have the following installed:
- **Java 17+** (JDK 18 is currently installed)
- **Maven 3.9.12** (installed at `C:\apache-maven-3.9.12`)
- **Python 3.x** (for serving the frontend)
- **Gemini API Key** (set as environment variable `GEMINI_API_KEY`)

## 🚀 Quick Start

### Step 1: Set Environment Variables

```powershell
# Set Java home and Maven path
$env:JAVA_HOME = "C:\Program Files\Java\jdk-18"
$env:PATH += ";C:\apache-maven-3.9.12\bin"

# Set Gemini API Key (required for transcription)
$env:GEMINI_API_KEY = "your-gemini-api-key-here"
```

### Step 2: Build Backend

```powershell
cd D:\Internship\audio-transcription-backend
mvn clean package -DskipTests
```

### Step 3: Start Backend Server

```powershell
cd D:\Internship\audio-transcription-backend
java -jar target\audio-transcription-backend-1.0.0.jar
```

The backend will start on **http://localhost:8080/api**

### Step 4: Start Frontend Server

```powershell
cd D:\Internship\circular-equalizer
python -m http.server 3000
```

The frontend will be available at **http://localhost:3000**

## 🎵 Using the Application

1. **Open the UI**: Navigate to http://localhost:3000 in your browser
2. **Start Microphone**: Click the "Start Microphone" button
3. **Grant Permissions**: Allow the browser to access your microphone
4. **View Visualization**: Watch the circular equalizer respond to audio input in real-time
5. **Live Transcription**: See real-time transcription from Gemini API in the right panel

## 📊 Architecture

### Backend (Spring Boot)

**Port**: 8080  
**Base Path**: /api

**Key Components**:
- **WebSocket Endpoint**: `/api/ws/transcribe` - Handles real-time audio streaming
- **Configuration**:
  - `application.yml` - Server configuration
  - `ResilienceConfig.java` - Circuit breaker and retry policies
  - `HttpClientConfig.java` - HTTP client setup
  - `WebSocketConfig.java` - WebSocket configuration

**Services**:
- `GeminiTranscriptionService` - Handles Gemini API communication
- `AudioProcessingService` - Processes incoming audio chunks
- `AudioStreamWebSocketHandler` - Manages WebSocket connections

### Frontend (HTML/CSS/JavaScript)

**Port**: 3000

**Components**:
- `index.html` - UI structure with canvas for visualization
- `style.css` - Responsive styling
- `script.js` - CircularEqualizer class with:
  - Web Audio API integration for frequency analysis
  - WebSocket connection to backend
  - Canvas rendering for circular visualization
  - Real-time transcription display

## 🔧 Configuration Files

### Backend Configuration (`application.yml`)

```yaml
# Gemini API
gemini:
  api:
    key: ${GEMINI_API_KEY}
    url: https://generativelanguage.googleapis.com/v1beta/models
    model: gemini-2.0-flash
    timeout-seconds: 30
    max-retries: 3

# Audio Processing
audio:
  chunk:
    size: 4096
    format: WAV
```

### Frontend Configuration (`script.js`)

```javascript
this.backendUrl = 'ws://localhost:8080/api/ws/transcribe';
```

## 🐛 Troubleshooting

### Backend Won't Start

1. Check if port 8080 is in use:
   ```powershell
   netstat -ano | findstr :8080
   ```

2. Ensure Java path is correct:
   ```powershell
   java -version
   ```

3. Check Gemini API key is set:
   ```powershell
   echo $env:GEMINI_API_KEY
   ```

### Frontend Can't Connect to Backend

1. Verify backend is running:
   ```powershell
   curl http://localhost:8080/api/actuator/health
   ```

2. Check browser console for WebSocket errors

3. Ensure firewall allows local connections

### No Audio Permission

1. Check browser microphone permissions
2. Ensure HTTPS or localhost (required for audio access)
3. Check browser console for MediaStream API errors

## 📈 Performance Notes

- **Backend**: Uses WebFlux for non-blocking I/O
- **Frontend**: Renders at 60 FPS with requestAnimationFrame
- **Audio**: Analyzes 2048 FFT size with 120 frequency bars
- **Transcription**: Real-time streaming from Gemini 2.0 Flash API

## 🔐 Security

- Backend is configured for local development (localhost:8080)
- API key stored as environment variable
- CORS not configured (local-only setup)

## 📝 Project Files

```
audio-transcription-backend/
├── pom.xml                           # Maven dependencies
├── src/main/java/com/audio/transcription/
│   ├── AudioTranscriptionApplication.java
│   ├── config/                       # Spring configuration
│   ├── controller/                   # REST endpoints
│   ├── service/                      # Business logic
│   ├── handler/                      # WebSocket handler
│   └── dto/                          # Data models
└── src/main/resources/
    └── application.yml               # Configuration

circular-equalizer/
├── index.html                        # UI structure
├── style.css                         # Styling
└── script.js                         # Visualization logic
```

## 📚 Key Technologies

- **Backend**: Spring Boot 3.2.1, WebFlux, WebSocket
- **Frontend**: HTML5 Canvas, Web Audio API, WebSocket
- **Resilience**: Resilience4j (Circuit Breaker, Retry)
- **API**: Gemini 2.0 Flash

## 🎓 Learning Resources

- [Spring Boot WebSocket](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [Web Audio API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API)
- [Gemini API](https://ai.google.dev/)
- [Resilience4j](https://resilience4j.readme.io/)
