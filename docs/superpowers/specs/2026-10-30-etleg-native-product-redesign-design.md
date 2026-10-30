# Etleg Native Product Redesign — Design

## Intent
Turn Etleg from a Telegram recolor into a recognizably independent Android messaging client with a coherent rounded, floating, calm visual system. Preserve Telegram's protocol/backend, account model, message delivery, calls, notifications and core navigation.

## Goals
- Replace the current Telegram-derived tab presentation with an Etleg-owned floating navigation surface and distinctive visual hierarchy.
- Replace the current Etleg settings shortcut page with a real, self-contained customization hub; no rows that merely forward into stock theme pages as the primary Etleg feature set.
- Add app-only settings that have observable behavior and persist locally.
- Remove Telegram branding from Etleg-owned surfaces and labels; retain factual attribution that Etleg is an unofficial client based on Telegram where legally/technically appropriate.
- Use custom, consistent iconography and rounded cards, with restrained teal/stone palette, no neon, gradients, or fake glass effects.
- Keep all changes package-gated to Etleg so shared upstream code paths remain unchanged for other builds.
- Add regression checks for branding, settings ownership, preference persistence, and navigation presentation.

## Non-goals
- Do not rewrite Telegram protocol, authorization, storage, message/call stack or server behavior.
- Do not remove core Telegram settings/features needed for account safety, privacy, data, sessions, notifications, calls, or accessibility.
- Do not use Premium-only animated emoji assets or unlock paid features. Use original vector icons that ship with the app.
- Do not claim Play Protect warnings are resolved without separate evidence.

## User experience
- Main navigation: a compact, raised, rounded Etleg dock with four familiar destinations (Chats, Contacts, Calls, Settings), a clear selected state, subtle motion, and no Telegram glass tab visual.
- Settings: an Etleg-branded landing page with a profile/identity header, grouped card sections, custom line icons, concise descriptions, switches and choice rows. Sections cover Interface, Chat behavior, Navigation, Privacy shortcuts and About. Each visible Etleg-specific control must work and persist.
- Branding: use Etleg name and launcher artwork on Etleg-owned screens; remove Telegram logo/name from the custom shell, while preserving required third-party attribution in About/legal text.
- Chat list: adjust Etleg-only row rhythm, spacing, rounded surfaces and divider treatment without replacing Telegram's dialog adapter or breaking gestures, folders, stories, topics, pinned/unread states.

## Constraints and defaults
- Android Java/XML/native views; no external runtime dependencies.
- Preserve existing app package gate `app.etleg.messenger`.
- Keep familiar tab order and destinations.
- Local preferences are app-only and must not sync to Telegram servers.
- Use the existing Android resource system and support Russian and English.
- Changes are developed on `etleg-full-interface-v2`; do not merge PR #4.

## Build approach
Keep the existing messaging engine and adapter contracts. Replace only Etleg-owned presentation surfaces: navigation layout, settings activity, appearance preference facade, and guarded chat-list rendering. Use native drawables/icons and preference-backed controls. Add regression tests before implementation and validate with the existing ARM64 GitHub Actions build.
