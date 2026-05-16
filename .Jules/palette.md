## 2024-05-16 - Add semantics modifier to Switch
**Learning:** Found an accessibility issue where standard Jetpack Compose Switch elements used in the TopAppBar of `ListeningScreen` had no descriptive labels.
**Action:** Next time, always check for inputs/controls, such as Switch components, to ensure they have `Modifier.semantics { contentDescription = "..." }` attached, or use standard labels so screen readers can accurately interpret the control's purpose.
