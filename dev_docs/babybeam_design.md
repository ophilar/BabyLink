# BabyBeam Design Decisions

## Architecture
- **Framework**: MVVM + Hilt + KSP.
- **AI**: Local-only inference using **LiteRT/TFLite Task Audio**.
- **Signaling**: **Google Nearby Connections** (P2P_STAR) to avoid cloud latency and maintain privacy.
- **Streaming**: WebRTC for low-latency audio transmission.

## UI/UX Guidelines
- **Theme**: Glassmorphism with deep blue/accent color (#4A90E2).
- **Icons**: AutoMirrored Material symbols for bi-directional support.
- **Accessibility**: Semantic headers and high-contrast alert overlays.

## Zenith Design Language
- **Integration**: Adopts the `ZenithStrategy` for a calming, breathable experience suitable for baby monitoring.
- **Iconography**: Uses the continuous-line Zenith Flow for the monochrome layer to symbolize connection and care.

## Security & Privacy
- **Zero Cloud**: No audio data leaves the local Wi-Fi/Bluetooth mesh.
- **Local Logs**: Alerts are stored purely in memory or encrypted local storage (Tink).
