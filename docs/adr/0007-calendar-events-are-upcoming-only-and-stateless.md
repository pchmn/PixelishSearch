# Calendar events are upcoming-only and stateless

Calendar search was built by mirroring the Contacts feature (a `calendar_search_enabled`
Preference gating a `READ_CALENDAR` runtime permission, a per-keystroke `CalendarRepository.search`,
a result row the size of `ContactResultRow`). It deliberately stops short of the Contacts template
in two ways, and this note records the *no*s so a future reader doesn't read them as oversights.

- **Upcoming-only.** Search queries `CalendarContract.Instances` over `now → now + 1 year` only.
  Past occurrences are never returned. A launcher search is forward-looking — you search to jump to
  an event you still need to attend — and `Instances` requires a bounded time range anyway, so an
  unbounded "all events" query isn't even free.
- **Stateless — no history, no Recents strip.** Unlike a Contact entry, a Calendar event has no
  `HistoryRepository` and does not participate in the blank-state Recents strip. A "recently opened
  event" is temporal: by the time it would resurface in Recents it may already be in the past, and
  there is no action to replay (tapping just opens it in the calendar app). With no usage score,
  ranking is the natural `Instances.BEGIN ASC` (soonest first), so ADR-0006's per-category `scoreOf`
  closure does not apply here.

A recurring series therefore contributes a single row — its next matching occurrence — by
deduplicating on `EVENT_ID` while iterating the `BEGIN`-ascending cursor.

## Revisit when

- Users ask to find *past* events ("the meeting I had last Tuesday") — at which point a bounded
  backward window and a past/future split in the date formatting would be needed.
- A reason emerges to make events replayable from the blank state, which would justify adding a
  `CalendarEventHistoryRepository` and folding events into the fused Recents strip.
