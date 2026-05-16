## 2024-05-16 - [Connection Flow Accessibility]
**Learning:** Found multiple areas where semantic intent wasn't clear to screen readers in connection flows. `Icon` components need `contentDescription` correctly mapped from their label or state, and actionable components acting as buttons must explicitly define `Role.Button` and `onClickLabel` using `semantics` or `clickable` modifiers.
**Action:** Always verify `Icon`s have descriptive labels matching the state, and use semantics/roles when making general Composables (like `Column`) interactive.
