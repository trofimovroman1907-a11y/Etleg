# Etleg Native Product Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship an Etleg-owned rounded native interface and genuinely useful app-specific settings without changing Telegram messaging behavior.

**Architecture:** Keep Telegram's protocol, fragments, adapters and data model. Add a package-gated Etleg appearance/preferences layer and replace the custom navigation/settings presentation with native Android views. Adjust dialog row presentation only behind the Etleg package gate.

**Tech Stack:** Android Java, Android resources, SharedPreferences, existing GitHub Actions ARM64 build, Python regression checks.

**Spec:** `docs/superpowers/specs/2026-10-30-etleg-native-product-redesign-design.md`

## Global Constraints
- Preserve `app.etleg.messenger` package gating.
- Keep Chats, Contacts, Calls, Settings destinations and existing Telegram backend.
- No external libraries, neon, gradients, glassmorphism, or Premium-only animated emoji.
- Do not merge PR #4.
- Russian and English resource strings required for every new visible label.

## Review Focus
- Non-Etleg packages must retain original navigation, settings, and dialog rendering.
- RTL and narrow Android screens must not clip navigation or settings rows.
- Preference defaults and malformed stored values must safely fall back.
- Returning from settings/theme changes must refresh visible controls and appearance.
- Existing dialog gestures, story rings, unread counters, folders, topics and selection states must remain intact.

---

### Task 1: Lock behavior with regression tests

**Files:**
- Modify: `tools/etleg_regression_checks.py`

- [ ] Add tests requiring Etleg settings to expose app-owned controls rather than redirect-only rows.
- [ ] Add tests requiring app-owned preference keys/defaults and package-gated access.
- [ ] Add tests requiring Etleg navigation to use a custom dock path while preserving four tab destinations.
- [ ] Run `python3 tools/etleg_regression_checks.py`; confirm the new tests fail for the missing implementation.

### Task 2: Build an Etleg preference and appearance facade

**Files:**
- Modify: `TMessagesProj/src/main/java/org/telegram/messenger/EtlegAppearance.java`
- Create: `TMessagesProj/src/main/java/org/telegram/messenger/EtlegPreferences.java`
- Test: `tools/etleg_regression_checks.py`

**Interfaces:**
- `EtlegPreferences.isEnabled(Context, String)`
- `EtlegPreferences.setEnabled(Context, String, boolean)`
- `EtlegPreferences.getDensity(Context)`
- `EtlegPreferences.setDensity(Context, int)`
- `EtlegAppearance.applyTheme()` continues to apply only to Etleg package.

- [ ] Implement typed, package-gated SharedPreferences access with safe defaults.
- [ ] Implement real controls for compact chat rows, message preview, floating dock, and reduced motion.
- [ ] Add tests for key names, defaults, package gating, and persistence API usage.
- [ ] Run regression checks.

### Task 3: Replace Etleg settings hub

**Files:**
- Modify: `TMessagesProj/src/main/java/org/telegram/ui/EtlegSettingsActivity.java`
- Modify: `TMessagesProj/src/main/res/values/strings.xml`
- Modify: `TMessagesProj/src/main/res/values-ru/strings.xml`
- Create: `TMessagesProj/src/main/res/drawable/etleg_*.xml` for original line icons.

- [ ] Replace redirect-only rows with a profile header, grouped rounded cards, original icons, switches and selectors.
- [ ] Implement settings actions using EtlegPreferences; no Premium emoji dependency.
- [ ] Keep an About/legal section that accurately identifies Etleg as an unofficial Telegram-based client.
- [ ] Ensure all strings are localized.
- [ ] Add tests for controls, localized labels, and no stock-theme redirect rows.
- [ ] Run regression checks.

### Task 4: Replace Etleg bottom navigation visual system

**Files:**
- Modify: `TMessagesProj/src/main/java/org/telegram/ui/MainTabsActivity.java`
- Modify: `TMessagesProj/src/main/java/org/telegram/ui/MainTabsLayout.java`
- Modify: `TMessagesProj/src/main/java/org/telegram/ui/Components/glass/GlassTabView.java` only if a clean package-gated custom rendering hook is required.

- [ ] Add a native Etleg dock with rounded elevated surface and a custom selected indicator.
- [ ] Retain four existing fragments, counters, click/long-press behavior, accessibility labels and RTL layout.
- [ ] Keep the upstream glass tab implementation untouched for non-Etleg builds.
- [ ] Add regression checks for package-gated dock construction and unchanged destination mapping.
- [ ] Run regression checks.

### Task 5: Refine chat-list row presentation

**Files:**
- Modify: `TMessagesProj/src/main/java/org/telegram/ui/Cells/DialogCell.java`
- Modify: `TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java` only for Etleg-specific list spacing/padding.

- [ ] Introduce restrained Etleg row spacing, separators and selected backgrounds behind the package gate.
- [ ] Keep avatar/story drawing, gestures, unread badges, topics, pinned state, swipe actions and accessibility unchanged.
- [ ] Add tests asserting Etleg gate around custom row metrics.
- [ ] Run regression checks.

### Task 6: Verify full product build

**Files:**
- Modify: `tools/etleg_regression_checks.py` as needed.

- [ ] Run all regression checks.
- [ ] Run the ARM64 GitHub Actions build for PR #4.
- [ ] Verify APK artifact and Etleg signing certificate.
- [ ] Inspect changed-file summary and confirm no PR merge occurred.
- [ ] Report exactly which screens and controls changed; do not label incomplete work as release-ready.
