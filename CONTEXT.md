# PixelishSearch

A unified search surface that combines several heterogeneous result categories
into a single list. The glossary below pins down what each category is so the
code identifiers, the user-facing strings, and the conversations about the
product all agree.

## Language

### Search result categories

**Tile**:
A quick-toggle surfaced inline in search results. Has binary state
(on/off), runs its action in-process or via a system Intent, and is drawn from
a small, hardcoded set declared in `settingsTiles`.
_Avoid_: Shortcut, quick setting, switch

**Settings page**:
A sub-page of the Android Settings app (e.g. Display, Sound, Modes),
discovered dynamically at preload by querying `ACTION_MAIN` in the Settings
package. Launched by `ComponentName`. Read-only entry point — no binary state,
no in-app action. Distinct from `SettingsPageHistoryEntry`, which is the
locally-stored recent-use record (not a source of truth — an entry whose
`ComponentName` is no longer in `SettingsPageIndex.entries` is hidden from
display until the page reappears).
_Avoid_: Settings shortcut, settings entry

**App entry**:
An installed, launchable application surfaced from the in-memory `AppIndex`.
_Avoid_: App, application, launcher entry

**App shortcut**:
A deep-link action a target app declares *statically* in its manifest
(`android.app.shortcuts` meta-data on a launcher activity, e.g. Files →
"Downloads", Fit → "Track workout"), surfaced from the in-memory
`ShortcutIndex` built by parsing every launcher app's shortcut XML — no
default-launcher role required. Only the *static* kind exists here: dynamic
and pinned shortcuts are out of scope because reading them needs the launcher
role. Searched live per keystroke; recorded in a `ShortcutHistoryRepository`
so recently-used shortcuts resurface in the fused blank-state Recents strip
alongside Contact entries and Settings pages (ADR-0002), and frequency
re-ranks typed results (ADR-0006) — matching the native Pixel Launcher, which
shows e.g. "Downloads · Files" there. Distinct from `ShortcutHistoryEntry`,
the locally-stored recent-use record (whose key is filtered from display once
its shortcut leaves `ShortcutIndex`). Distinct from a **Tile** (which
deliberately avoids the word "shortcut") and from `pinAppShortcut` (which pins
an *app* to the home screen via Android's pinned-shortcut API — an unrelated
sense of the word).
_Avoid_: Shortcut (bare — ambiguous with the Tile alias and the pinned-app
sense), dynamic shortcut, pinned shortcut, static shortcut (in user-facing
text; the "static" qualifier is an implementation detail)

**Contact entry**:
A person surfaced from the device address book via `ContactRepository`.
Distinct from `ContactHistoryEntry`, which is the locally-stored recent-use
record (not a source of truth).
_Avoid_: Contact (ambiguous between source and history record)

**Calendar event**:
A single upcoming occurrence surfaced from the device calendars via
`CalendarRepository`. Searched live per keystroke like a Contact entry; only
occurrences from now onward are returned, and a recurring series contributes
its next matching occurrence rather than the abstract series. Tapping one opens
it in the calendar app — it carries no quick actions of its own.
_Avoid_: Event (overloaded with Android/analytics events), appointment,
meeting, agenda item

**Web suggestion**:
A query string returned by Google Suggest for the current input.
_Avoid_: Search suggestion, autocomplete

### App configuration

**Preference**:
A configuration value of PixelishSearch controlled by the user and
persisted in the app's DataStore (e.g. contact search enabled, the set of
disabled Tile IDs, the chosen language). Distinct from a *Setting*, which
in this codebase always refers to the Android system Settings app (and the
Tiles and Settings pages it exposes to search). The Preferences screen is
*labelled* "Settings" in the UI by Android convention, but its content is
Preferences.
_Avoid_: Setting, config, option (when meaning a user-controlled value of
PixelishSearch)

### Visibility vs runtime state

The word "state" is overloaded for Tiles. We distinguish:

**Visibility**:
Whether a Tile appears in search results at all. Controlled by a Preference.
Persisted as the *disabled* set in DataStore — empty set = all visible.

**Runtime state**:
A Tile's current on/off value (WiFi on, Bluetooth off, …). Read from the
system at query time via `SettingsTileId.isActive(context)`. Not persisted by
PixelishSearch.

A Tile can be visible-but-off, visible-and-on, or hidden (in which case its
runtime state is irrelevant).

### Recents strip

**Recents strip**:
The blank-state suggestion list shown when the query is empty. Mixes recent
App entries, fused Contact + Settings page entries, etc.
_Avoid_: Suggestions strip, recents list

**Recents-hidden app**:
An App entry the user has excluded from the Recents strip. Remains fully
searchable when typed — exclusion is scoped to the blank-state strip only.
Distinct from a disabled Tile, which disappears from search entirely.
_Avoid_: Hidden app (ambiguous between recents-exclusion and full
invisibility), disabled app

## Example dialogue

> **Dev**: Should "Display" show up when I type "dis"?
> **Domain**: That's a Settings page, not a Tile — different category. Tiles
> are only the ten in `settingsTiles`; Settings pages are whatever
> `SettingsPageIndex` discovers on the device.
>
> **Dev**: OK, and if the user disables the WiFi tile in app Settings, does
> that turn WiFi off?
> **Domain**: No — disabling a Tile changes its **visibility**, not its
> **runtime state**. WiFi keeps doing whatever it was doing; we just stop
> rendering the tile in search results.
>
> **Dev**: And if I long-press Slack and "hide" it, does it disappear from
> search too?
> **Domain**: No — that makes it a **Recents-hidden app**: Slack drops out
> of the Recents strip (blank query) but still matches normally when you
> type "sla". Different from a disabled Tile, which would vanish from
> search entirely.
