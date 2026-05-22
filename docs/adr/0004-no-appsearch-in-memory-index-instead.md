# Use in-memory indexes + DataStore JSON, not Jetpack AppSearch

Search-relevant data (Apps, Contacts, Settings pages, Recents histories) lives
in feature-owned in-memory structures hydrated from DataStore JSON blobs or
queried live against system ContentProviders. We do not use
[Jetpack AppSearch](https://developer.android.com/develop/ui/views/search/appsearch).

The architectural goal stated in `AGENTS.md` is ultra-fast cold start.
AppSearch's `createSearchSession` opens a RocksDB-backed store and forces every
query into an async `ListenableFuture` boundary — directly at odds with the
current Phase A hydration (`AppIndex.preload` reads one JSON string from
DataStore and exposes the decoded list synchronously through a hot
`StateFlow`).

Per-category rationale:

- **Apps** (~200 entries): in-memory `startsWith` / `contains` over a
  normalized label runs in microseconds. AppSearch's BM25 / tokenizer offers no
  user-visible improvement over the Pixel-Launcher-style matching we
  reproduce, and lacks the exponential-decay usage ranking we apply via
  `HistoryEntry.score()`.
- **Contacts**: the device `ContactsContract` ContentProvider is the canonical,
  system-indexed source of truth. Mirroring it into AppSearch would introduce
  staleness, duplicate the permission cost, and pay a re-indexing cost on
  every contact change for zero search-quality gain.
- **Recents histories** (`HistoryRepository<T>`, capped at 20 entries per
  category): never queried by text — only displayed, ordered by
  `lastUsedEpochMillis`, and reused as `scoreOf(id)` for ranking live results.
  A hot `StateFlow` is strictly faster than any async search session.
- **Settings pages** (~30 entries): same shape as Apps, same conclusion.

## Considered options

- **Adopt AppSearch (LocalStorage) for all search-relevant data** — rejected:
  adds async session init at cold start, replaces sync µs-level lookups with
  async queries, and provides no ranking primitive matching our
  usage-count × exponential-time-decay model (we would still have to store and
  sort by our own properties in Kotlin).
- **Adopt AppSearch (PlatformStorage) to expose indexes to Assistant / other
  apps** — rejected: PixelishSearch is a closed search surface; nothing
  consumes our index externally. The cross-process overhead would buy nothing.

## Revisit when

- We carry tens of thousands of heterogeneous items to search (not the case
  today — total searchable corpus is ≲ 300 entries across all categories).
- We want to expose an index to Google Assistant or another app via
  `PlatformStorage`.
- We need full-text relevance over rich descriptive fields (notes, aliases,
  organisation, etc.) that the current label-only matching cannot serve.
