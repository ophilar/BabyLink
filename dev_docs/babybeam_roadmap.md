# BabyBeam Project Roadmap

## Phase 1: Foundation & Build (COMPLETE)
- [x] Scaffolding with Hilt & KSP.
- [x] UI Design integration with `FluxZenShared`.
- [x] Manifest stability & Theme mapping.
- [x] JUnit 5 test suite setup.
- [x] Glassmorphic Iconography.

## Phase 2: Core Monitoring Logic (IN PROGRESS)
- [x] `BabyMonitorViewModel` implementation with signaling.
- [x] `NearbyTransportLayer` integration for multi-device discovery.
- [x] Cry Detection trigger logic.
- [x] Audio Stream Processing (PCM Feeding).
- [ ] Background Service persistence.

## Phase 5: Zenith Design Language ✅
- [x] **Icon Modernization** - Implement Vector-Only adaptive icons.
- [x] **Monochrome Compliance** - Strict Material You theming support.
- [x] **Zenith UI Strategy** - Apply organic flow to baby monitoring dashboard.

## Phase 3: UI & UX Polish
- [ ] Interactive waveform visualization.
- [ ] Lullaby synchronization across devices.
- [ ] Night mode / Light sensor triggers.

## Phase 4: Production Alignment
- [x] **Zero-Maintenance Pattern** - Local/Remote source alignment for `FluxZenShared`.
- [ ] **16KB Alignment migration** (LiteRT 2.0.2+, WebRTC M121+).
- [ ] **Security Hardening** - Migrate to Android Keystore & Sign all signaling messages.
- [ ] Release ProGuard/R8 hardening.
- [ ] Google Play deployment metadata.

## Progress Update - 2026-05-29
- [x] Concluded PR Audit and Consolidation.
- [x] Merged Composable UI navigation separation (PR 17).
- [x] Ported high-performance, background-threaded signaling and signature handling to BabyMonitorViewModel (PR 16/20).
- [x] Ported ContextCompat vibrator refactoring (PR 14) and RoleSelection permissions cleanup (PR 19).
- [x] Cleaned up all mocked/faked unit tests to comply with the new test engineering standard.

