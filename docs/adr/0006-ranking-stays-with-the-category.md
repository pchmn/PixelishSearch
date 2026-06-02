# Ranking stays with the category, not in a centralized Ranker module

Each search category owns its ranking. `HistoryEntry.score()` is the only
shared piece. Per-category sort lives where the category lives:

- **Blank state** — `<Feature>HistoryRepository.ranked(...)` (e.g.
  `AppHistoryRepository.ranked(apps)`), reading the repo's own
  `byKey: StateFlow<Map<K, T>>` from `HistoryRepository`.
- **Per-keystroke** — `XxxIndex.search` / `XxxRepository.search` takes a
  `scoreOf: (K) -> Float` closure and applies `sortedWith` internally; the
  VM threads `repo.byKey.value` into the closure.

No top-level `Ranker` module.

## Considered options

- **Centralize ranking in `core/data/Ranker.kt`** with a `Buckets<T>` type,
  a generic `Buckets<T>.rankByHistory` engine, and per-category typed
  overloads — rejected: redistributive, not deepening. Replaced ~28
  distributed lines of idiomatic `compareByDescending { scoreOf(key) }` with
  ~84 centralized lines plus a new concept (`Buckets<T>`) and four
  `@JvmName` annotations to disambiguate JVM-erased generics. Reading "how
  do apps rank?" went from one file to 3–4. The deletion test failed —
  removing the module restored short, locally readable idioms.

## Revisit when

- A fifth or sixth category appears and per-category ranking sites visibly
  duplicate logic that isn't already idiomatic Kotlin.
- Ranking gains a non-trivial second dimension (semantic similarity, bigram
  boosts) that no longer fits in a short `compareByDescending` chain at
  each call site.
