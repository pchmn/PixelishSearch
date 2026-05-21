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
no in-app action.
_Avoid_: Settings shortcut, settings entry

**App entry**:
An installed, launchable application surfaced from the in-memory `AppIndex`.
_Avoid_: App, application, launcher entry

**Contact entry**:
A person surfaced from the device address book via `ContactRepository`.
Distinct from `ContactHistoryEntry`, which is the locally-stored recent-use
record (not a source of truth).
_Avoid_: Contact (ambiguous between source and history record)

**Web suggestion**:
A query string returned by Google Suggest for the current input.
_Avoid_: Search suggestion, autocomplete

### Visibility vs runtime state

The word "state" is overloaded for Tiles. We distinguish:

**Visibility**:
Whether a Tile appears in search results at all. User-controlled in the app
Settings. Persisted as the *disabled* set in DataStore — empty set = all
visible.

**Runtime state**:
A Tile's current on/off value (WiFi on, Bluetooth off, …). Read from the
system at query time via `SettingsTileId.isActive(context)`. Not persisted by
PixelishSearch.

A Tile can be visible-but-off, visible-and-on, or hidden (in which case its
runtime state is irrelevant).

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
