---
name: sync-upstream-release
description: Merge a selected azure-tools-for-java release branch into the Azure Toolkit fork, update its upstream-release metadata and changelog, then open a pull request.
user_invocable: true
---

# Sync an Upstream Release

Use this skill when the user asks to bring a release branch from `microsoft/azure-tools-for-java` into the JetBrains fork. Work from the repository root returned by `git rev-parse --show-toplevel`, not the Rider plugin directory. In this repository, the workflow is at the root (`.github/workflows/build.yml`) and the Rider changelog is at `PluginsAndFeatures/azure-toolkit-for-rider/CHANGELOG.md`.

## Required input and safety checks

1. Ask the user for the exact upstream release branch before changing repository state. Accept the forms `release-vX.Y.Z` and `release-X.Y.Z`, for example `release-v3.97.1` or `release-3.97.6`; do not infer, substitute, normalize, or select a release on the user's behalf. Preserve the selected branch name exactly throughout the workflow.

2. Check that the worktree is clean with `git status --porcelain`. If it is not clean, stop and report the paths. Do not stash, discard, overwrite, or include another person's changes.

3. Ensure the `upstream` remote exists. If it is absent, add exactly:

   ```powershell
   git remote add upstream git@github.com:microsoft/azure-tools-for-java.git
   ```

   If `upstream` exists but has a different URL, stop and ask the user to resolve that configuration.

4. Fetch the upstream refs:

   ```powershell
   git fetch upstream
   ```

5. Verify that `upstream/<selected-release>` exists. If it does not, stop and report that the requested release is unavailable. Also stop if either the local branch `upstream/<selected-release>` or the identically named branch on `origin` already exists; do not overwrite an existing integration branch.

## Create the integration branch

Create `upstream/<selected-release>` directly from `upstream/<selected-release>`, then merge the local `develop` branch into it:

```powershell
git switch --create "upstream/<selected-release>" "upstream/<selected-release>"
git merge develop --no-edit
```

If the merge reports conflicts, first list all unmerged paths and inspect every conflicted hunk without resolving any of them. Resolve conflicts automatically only when every conflicted hunk is covered by one or more of these exceptions:

- For paths under `.azure-pipelines/`, match the local `develop` branch exactly.
- When an entire class is active in the upstream release but commented out in `develop`, keep the complete commented-out version from `develop`.
- When an individual line is active in the upstream release but commented out in `develop`, keep that line in its commented-out form. Preserve the rest of the merged content unless another listed exception applies to it.

In this merge, `develop` is Git's `theirs` side, not `ours`. For a path that must match `develop` completely, restore and stage it when it exists in `develop`, and use `git rm` when it was deleted in `develop`. For individual commented lines, resolve only the affected hunks while retaining the commented form from `develop`. Confirm that the resolved content preserves those comments and that no unmerged paths remain, then complete the merge with `git commit --no-edit` and continue.

If any conflicted hunk is not covered by these exceptions, run `git merge --abort` without resolving any conflict. Do not update metadata, push, or create a pull request. Leave the newly created branch available at its upstream base and hand control to the user with the conflict report.

If the merge succeeds, retain its result and continue.

## Update fork metadata

Update only the following files after a successful merge:

1. In `.github/workflows/build.yml`, set the single `UPSTREAM_RELEASE_VERSION` value by removing the optional `v` after `release-` and replacing the version dots with hyphens. For example, both `release-v3.97.1` and `release-3.97.1` become:

   ```yaml
   UPSTREAM_RELEASE_VERSION: release-3-97-1
   ```

2. In `PluginsAndFeatures/azure-toolkit-for-rider/CHANGELOG.md`, add the selected upstream release under the `[Unreleased]` section's `Changed` subsection, creating that subsection if absent:

   ```markdown
   - Sync plugin with upstream `release-v3.97.1`.
   ```

3. Synchronize the Rider plugin's library versions in `PluginsAndFeatures/azure-toolkit-for-rider/gradle/libs.versions.toml` with the merged upstream source of truth in `Utils/pom.xml`:

   - Set `azureToolkitLibs` to the value of `<azure.toolkit-lib.version>`.
   - Set `azureToolkitHdinsightLibs` to the value of `<hdinsight.toolkit-ide-lib.version>`.

   Do not update any other version-catalog entries. If either source property is absent or has no value, stop and report it rather than guessing a version.

Do not change unrelated workflow settings, changelog entries, or version-catalog entries. Run `git diff --check` after the edits.

## Publish and open the pull request

Commit the metadata and Rider library-version changes in a separate commit with a clear message, such as `chore: sync upstream release-v3.97.1`. Push the integration branch to `origin`, then create a GitHub pull request in `JetBrains/azure-tools-for-intellij` from that branch into `develop`. Use a clear title such as `Sync upstream release-v3.97.1` and mention the selected upstream branch in the PR body.

Report the upstream branch, integration branch, merge result, files changed, commit SHA, and PR URL. If authentication or GitHub CLI access prevents pushing or creating the PR, stop and report the exact failed command and error; do not retry with a different remote or publish destination.
