---
name: write-release
description: >
  Build a signed release APK and publish (or update) a GitHub Release for PixelishSearch.
  Reads `versionName` from `app/build.gradle.kts`, runs `./gradlew :app:assembleRelease`,
  collects commits since the previous tag, updates `CHANGELOG.md`, tags the commit, and
  creates the GitHub Release via `gh` with the APK uploaded as `pixelish-search-vX.X.X.apk`.
  Automatically detects whether a release already exists for the current version: if yes,
  updates it; if no, creates it. Use this skill whenever the user wants to publish a new
  release, ship a build, cut a version, push an APK to GitHub Releases, or says things like
  "create a release", "ship v1.0.0", "publish the APK", "update the release", or "/write-release".
---

# Release Skill

## Interaction Rules

- For all confirmations, choices, and decision points: use the agent's native interactive
  question/choice tool to present selectable options. Do not list options as plain text — the user
  must be able to click/select, not type.

Build a signed release APK and publish it as a GitHub Release. The skill reads the current
`versionName` from `app/build.gradle.kts`, asks the user which version to ship (with a
recommended bump), writes the new value back, then builds and publishes.

## Workflow

1. **Read state.** Extract current `versionName` from `app/build.gradle.kts` and collect commits
   since the previous tag with `git log <previous-tag>..HEAD --pretty=format:"%H%x09%s"` (use
   `git describe --tags --abbrev=0` to find it; full history if none). You'll reuse these
   commits in step 7.

   The repo uses **squash-merge**, so on `main` each PR collapses to a single commit whose
   subject is the PR title and ends with `(#NN)` (added by GitHub). One commit on `main` = one
   PR = one changelog line. Never try to fetch the original commits inside the PR — they don't
   exist on `main`.

2. **Ask the user for the next version.** Compute a recommended bump from the commits using
   conventional-commits rules:
    - any `feat:` → minor bump (`1.0.0` → `1.1.0`)
    - only `fix:`/`perf:`/`chore:`/etc. → patch bump (`1.0.0` → `1.0.1`)
    - any `BREAKING CHANGE` footer or `!:` in the subject → major bump
    - if current version is a prerelease (`-beta.N`, `-rc.N`, `-alpha.N`), default to
      incrementing the prerelease counter (`1.0.0-beta.2` → `1.0.0-beta.3`)

   Present selectable options: recommended bump first (labelled "Recommended"), the other two
   bumps, and "Custom" (free-form version). Store the bare version, no `v` prefix.

3. **Write the new `versionName` to `app/build.gradle.kts`.** Targeted `Edit` on the
   `versionName = "..."` line inside `defaultConfig`. Do not touch `versionCode` — the user
   manages that. Stop and warn if the resulting tag `v<new-version>` already exists.

4. **Check branch & cleanliness.** Confirm we're on `main` (or offer "continue / switch /
   cancel"). The `versionName` edit from step 3 is expected to be uncommitted — don't treat it
   as dirty. If there are *other* uncommitted changes, offer "Commit first" / "Continue
   anyway" / "Cancel".

5. **Detect create vs. update.** Run `gh release view v<new-version>` to check if a release
   already exists. Tell the user which flow you're following.

6. **Build the APK.** Run `./gradlew :app:assembleRelease`. The signed APK lands in
   `app/build/outputs/apk/release/app-release.apk`. If the build fails (missing
   `keystore.properties`, signing error, compile error), stop and surface the error — don't try
   to recover by switching to debug.

7. **Update `CHANGELOG.md`.** Use the commits from step 1, grouped by conventional-commits type:
    - `feat:` → **Features**
    - `fix:` → **Bug Fixes**
    - `perf:` → **Performance**
    - `refactor:`, `chore:`, `docs:`, `test:`, `build:`, `ci:` → **Other** (omit `chore`/`docs`
      if they add no user-visible value)

   For each commit, link the commit SHA and the PR (if one exists). Get commits with
   `git log <previous-tag>..HEAD --pretty=format:"%H%x09%s"` to keep the full SHA, then resolve
   the PR for each commit with `gh pr list --search "<sha>" --state merged --json number,url`
   (or parse the `(#NN)` suffix that squash-merge leaves in the subject — faster, no API call).
   Use the repo's `<owner>/<repo>` from `gh repo view --json nameWithOwner` to build URLs.

   Prepend a new section to `CHANGELOG.md` (create the file if missing — use the
   [Keep a Changelog](https://keepachangelog.com/) header). Format (each bullet ends with the
   short SHA linked to the commit, then `(#NN)` linked to the PR if one exists):

   ```markdown
   ## [v1.0.0-beta.2] - 2026-05-23

   ### Features
   - Short imperative description ([`a1b2c3d`](https://github.com/<owner>/<repo>/commit/a1b2c3d4...)) ([#42](https://github.com/<owner>/<repo>/pull/42))

   ### Bug Fixes
   - ...
   ```

   Strip the conventional-commits prefix and any trailing `(#NN)` from the subject before
   appending the links. Skip merge commits and release commits themselves.

8. **Commit the version bump + changelog & tag.** Stage `app/build.gradle.kts` and
   `CHANGELOG.md`, commit with `chore: release v<new-version>`, then create the annotated tag
   `git tag -a v<new-version> -m "Release v<new-version>"`. Push both with
   `git push && git push origin v<new-version>`.

9. **Publish the release.** Rename the APK to `pixelish-search-v<new-version>.apk` (use the
   `file#label` syntax of `gh release create`, no need to actually copy). The release notes are
   the changelog section you just wrote (pass via `--notes-file` after extracting it, or
   `--notes` with the markdown content). Mark prereleases (`-beta`, `-rc`, `-alpha` in the
   version) with `--prerelease`.

    - **Create**:
      `gh release create v<new-version> "app/build/outputs/apk/release/app-release.apk#pixelish-search-v<new-version>.apk" --title "v<new-version>" --notes "<changelog-section>" [--prerelease]`
    - **Update**: `gh release edit v<new-version> --notes "<changelog-section>"` then
      `gh release upload v<new-version> "app/build/outputs/apk/release/app-release.apk#pixelish-search-v<new-version>.apk" --clobber`

10. **Show the release URL** returned by `gh` when done.

## Notes

- Never bypass signing or fall back to debug builds — a release without the proper keystore is
  useless.
- Never force-push the tag or delete an existing release without explicit user confirmation.
- The APK path is fixed by Gradle output conventions; don't search for it.
