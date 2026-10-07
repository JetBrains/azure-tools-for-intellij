---
name: update-intellij-sdk-eap
description: Update Azure Toolkit for Rider to the newest Rider EAP in a selected release line, then build and repair clear SDK-upgrade regressions.
user_invocable: true
---

# Update Rider SDK EAP

Update the Rider SDK used by this repository only when the user has selected the target EAP release line.

## Input

Before making a network request, editing a file, or starting a build, ask the user for the target EAP release line in the exact form `YYYY.N`, for example `2026.3`. Do not infer the release line or proceed without this input.

## Steps

1. Read `platformVersion` and `pluginVerificationIdeVersion` from the repository-root `gradle.properties`. Preserve unrelated working-tree changes. Both properties are Rider versions: the first configures the `rider(...)` dependency and the second configures Rider Plugin Verifier.

2. Fetch and parse `https://jb.gg/intellij-platform-builds-list` as JSON. From the `RD` product, select the first release whose `type` is `eap` and whose `version` matches the requested release line followed by `-EAP` and its EAP number. For example, `2026.3` matches `2026.3-EAP3`. The list is newest-first, so this is the newest Rider EAP for the requested release line. If no matching release exists, stop and report it; do not substitute another release line.

3. Form the Gradle version by appending `-SNAPSHOT` to the selected Rider release version; for example, `2026.3-EAP3` becomes `2026.3-EAP3-SNAPSHOT`. Update only these two properties in `gradle.properties` to that same Gradle version:
   - `platformVersion`
   - `pluginVerificationIdeVersion`

   Do not add or update an IntelliJ IDEA version, `riderVersion`, or `ideaVersion`: this plugin depends on Rider only. Do not change `pluginSinceBuild` as part of an EAP update.

4. Run the existing **Build Plugin** run configuration in `.run/Build Plugin.run.xml`. It invokes the Gradle `buildPlugin` task. If direct IDE run-configuration execution is unavailable, run the equivalent command from the repository root:

   ```powershell
   .\gradlew.bat buildPlugin
   ```

   Do not run `test`, `check`, or JVM integration tests for this workflow.

5. When the build fails because of the SDK update, inspect the actionable Gradle or compiler error and make only a confident, minimal compatibility fix. Do not edit generated sources, weaken or mute validation, downgrade the selected SDK, or change unrelated code. Re-run **Build Plugin** after each fix.

6. If the cause or fix is not immediately clear, stop instead of guessing. Report the requested release line, selected Rider EAP, property changes, build result, exact error location/output, and any fixes already attempted.

7. After **Build Plugin** succeeds, update `CHANGELOG.md` under `[Unreleased]` → `Changed`. Add:

   ```markdown
   - Update platform version to RD-<Gradle Rider version>
   ```

   For example, for Rider `2026.3-EAP3`, add `- Update platform version to RD-2026.3-EAP3-SNAPSHOT`. Create the `Changed` subsection if it is absent.

## Completion

On success, report the previous and new values of both Rider-version properties, the selected upstream Rider EAP, the **Build Plugin** result, the changelog entry, and the files changed. Run `git diff --check` before reporting completion.
