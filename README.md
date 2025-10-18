# Codex Android App – Developer Handbook

## Quick Facts
- **Target SDK**: 34 (`compileSdk` 34, `targetSdk` 34).
- **Minimum SDK**: 21.
- **Language**: Java 17 across the codebase.
- **Entry Points**: `MainActivity` (`app/src/main/java/com/codex/apk/MainActivity.java`) launches the workspace UI; `EditorActivity` (`app/src/main/java/com/codex/apk/EditorActivity.java`) provides the core editor + chat experience.
- **Application Class**: `CodeXApplication` (`app/src/main/java/com/codex/apk/CodeXApplication.java`) sets global theme and crash handling.

## Repository Layout (app/)
```
app/
├── build.gradle
├── codex.keystore
├── proguard-rules.pro
└── src/main/
    ├── AndroidManifest.xml
    ├── assets/
    ├── java/com/codex/apk/
    │   ├── ai/
    │   ├── editor/
    │   │   └── adapters/
    │   ├── lint/
    │   ├── util/
    │   └── *.java (activities, fragments, managers, adapters)
    └── res/
        ├── drawable/
        ├── layout/
        ├── menu/
        ├── mipmap/
        ├── values/
        └── xml/
```

> Only files under `app/` are documented here, per project guidance.

## Build & Run Checklist
- **Tooling**: Android Studio Flamingo+ (Gradle 8.x), Android SDK 34, JDK 17.
- **Clone**: `git clone` then open the root in Android Studio. Import as Gradle project.
- **API Credentials**: Configure via `SettingsActivity` UI (runtime) or SharedPreferences (debug). Providers like Gemini require API keys or cookies; refer to class-level KDoc in `GeminiFreeApiClient` and `GeminiOfficialApiClient` for cookie/key names.
- **Signing**: A debug keystore is bundled as `app/codex.keystore` (password `codex123`). If removing from VCS, update `build.gradle` to match your keystore.
- **Gradle Sync**: Ensure sync completes; dependencies are published to Maven Central or GitHub packages (no local AARs required).
- **Run**: Choose an emulator/device on API 21+ and launch.

## Application Architecture Overview

- **Presentation Layer**: Activities and fragments coordinate UI. `MainActivity` handles project list navigation; `EditorActivity` hosts `CodeEditorFragment` (code view) and `AIChatFragment` (assistant view).
- **Manager Layer**: High-level orchestrators encapsulate domain flows.
  - `AIAssistantManager` (`editor/AiAssistantManager.java`) links AI events to UI updates and tool execution.
  - `ProjectManager` (`ProjectManager.java`) and `ProjectImportExportManager` manage workspace persistence.
  - `PlanExecutor` (`editor/PlanExecutor.java`) drives AI-generated plans.
- **Domain Layer**: Models in `ai/` describe providers, models, capabilities, and prompt composition.
- **Infrastructure Layer**: `ApiClient` implementations (`QwenApiClient`, `GeminiFreeApiClient`, etc.) perform network I/O with OkHttp, streaming via `SseClient`.
- **Utilities**: Helpers in `util/` (`FileOps`, `JsonUtils`, `ResponseUtils`) consolidate cross-cutting concerns.
- **Lint & Tools**: `lint/` package includes simple HTML/CSS/JS linting; `ToolExecutor` runs AI-suggested file ops.

The architecture does not enforce MVVM; responsibilities bleed between UI and managers. Future work should introduce ViewModels and dependency injection to tighten contracts.

## Startup Sequence
1. `CodeXApplication.onCreate()` applies theming via `ThemeManager.setupTheme()` and installs a crash handler launching `DebugActivity` (`DebugActivity.java`).
2. `MainActivity` loads recent projects using `ProjectManager` and `ProjectsAdapter`.
3. Selecting a project starts `EditorActivity`, which wires `AiAssistantManager`, `FileTreeManager`, `TabManager`, and `CodeEditorFragment`.
4. `EditorActivity` attaches `AIChatFragment`; `AIChatUIManager` binds RecyclerViews and view bindings.
5. `AIAssistant` initializes provider clients (`initializeApiClients()`), reading persisted keys from `SettingsActivity` helpers.

## Feature Walkthroughs

### AI Chat + Tooling Flow
- **Entry**: User submits prompt through `AIChatFragment` (`AIChatFragment.java`).
- **Routing**: `AIAssistant.sendMessage()` chooses `ApiClient` using `AIModel.getProvider()`.
- **Network**: Providers stream via `SseClient.postStreamWithRetry()` (see `QwenApiClient.java` and `QwenStreamProcessor.java`).
- **Parsing**: `QwenResponseParser` extracts actions, plans, tool suggestions. Other clients perform inline JSON parsing with Gson.
- **UI Update**: `AiAssistantManager` posts updates to `ChatMessageAdapter`, toggling plan cards, file diffs, and tool usage.
- **Tool Execution**: `ToolExecutor` orchestrates file modifications, calling helpers in `FileOps` and diff utilities (`DiffGenerator`, `DiffUtils`).

Key data class: `ChatMessage` stores raw responses, plan steps, thinking content, web sources, and tool usage for rendering.

### Project Lifecycle
- `ProjectManager` supports creating, opening, renaming, duplicating, and deleting projects under the workspace root.
- `GitManager` wraps JGit commands for clone, commit, push, and diff operations.
- `ProjectImportExportManager` zips/unzips workspaces, coordinating with `AdvancedFileManager` for storage access and SAF prompts.
- Recycler adapters (`ProjectsAdapter`, `RecentProjectsAdapter`) render project items.

### Code Editing & Diffing
- `CodeEditorFragment` hosts a Rosemoe editor widget (syntax highlighting via BOM `io.github.rosemoe:editor-bom`).
- `TabManager` maintains multiple open files (`TabItem` models) with state persistence.
- Diff visualizations use `PreviewActivity`, `InlineDiffAdapter`, and `SplitDiffAdapter` with support from `DiffGenerator`.
- Markdown previews rely on `MarkdownFormatter` leveraging Markwon modules.

### Settings & Model Management
- `SettingsActivity` exposes preferences for API keys, cookies, theme, and feature toggles (agent mode, web search, thinking mode).
- `ModelsActivity` displays provider/model combinations using `ModelAdapter`; `AIAssistant.refreshModelsForProvider()` fetches updates asynchronously.
- Provider metadata defined by `AIModel`, `AIProvider`, and `ModelCapabilities` enumerations.

## AI Provider Abstraction Layer

`ApiClient` (`ApiClient.java`) defines the contract for provider integrations. Current implementations:

| Provider | Class | Notes |
| --- | --- | --- |
| Alibaba Qwen | `QwenApiClient.java` | Stateful conversations (`QwenConversationManager`, `QwenMidTokenManager`), SSE streaming, tool actions.
| Gemini Free (cookies) | `GeminiFreeApiClient.java` | Reverse-engineered endpoints, cookie rotation (`ROTATE_COOKIES_URL`).
| Gemini Official | `GeminiOfficialApiClient.java` | API key from settings, JSON payloads via OkHttp.
| DeepInfra | `DeepInfraApiClient.java` | Multiplex provider fallback, uses JSON completions.
| AnyProvider | `AnyProviderApiClient.java` | Fallback aggregator for community endpoints.
| OpenRouter | `OpenRouterApiClient.java` | Market-style provider; supports tool metadata.
| OIVSCodeSer0501 | `OIVSCodeSer0501ApiClient.java` | Custom endpoint for OSS mirror.
| WeWordle | `WeWordleApiClient.java` | Specialized chat with unique headers.

Shared concerns:
- All create an `OkHttpClient`, often with duplicated timeout settings.
- Many spawn `new Thread` for network calls; consider centralizing to `ExecutorService`.
- Error propagation uses `AIAssistant.AIActionListener` callbacks for UI updates.

## Core Data & State Objects
- `ChatMessage`: Chat payload with message types, plan steps (`ChatMessage.PlanStep`), tool usage, and file change proposals.
- `QwenConversationState`: Tracks conversation IDs and thread tokens for Qwen.
- `ToolSpec`: Declares tool metadata (name, description, parameters) consumed by AI planning.
- `AIModel`/`ModelCapabilities`: Define provider-specific features (streaming, tool support, file context).

## Managers, Utilities, and Helpers
- `AiAssistantManager`: Coordinates AI calls, attaches plan execution, handles offline checks via `ConnectivityManager`.
- `FileOps`: Read/write/copy, BOM handling, safe file rename operations.
- `AdvancedFileManager` and `FileManager`: Bridge between Android storage APIs and internal file operations.
- `PromptManager`: Supplies system prompts for general vs. agent mode usage.
- `TemplateManager`: Stores canned templates/snippets.
- `ToolExecutor`: Executes plan steps, applying diffs with `DiffUtils` and `DiffGenerator`.
- `LocalServerManager`: Spins up local endpoints for preview or callbacks.

## UI Layer Breakdown
- **Activities**: `MainActivity`, `EditorActivity`, `SettingsActivity`, `ModelsActivity`, `AboutActivity`, `ApiActivity`, `PreviewActivity`, `DebugActivity`.
- **Fragments**: `AIChatFragment`, `CodeEditorFragment`.
- **Adapters**: `ChatMessageAdapter`, `FileActionAdapter`, `ProjectsAdapter`, `RecentProjectsAdapter`, `ModelAdapter`, `SimpleSoraTabAdapter`, `SplitDiffAdapter`, `InlineDiffAdapter`, `WebSourcesAdapter`, `editor/adapters/MainPagerAdapter`.
- **Layout Resources**: Found under `app/src/main/res/layout/`, each adapter references specific item layouts (e.g., `item_chat_message_ai`, `item_plan_step`).

Heavy adapters (notably `ChatMessageAdapter`) contain extensive binding logic (>600 LOC). Future work should divide responsibilities into dedicated view binders or Compose UI modules.

## Asynchronous Patterns
- Uses raw `new Thread` for network tasks (`QwenApiClient`, `AIAssistant`, `GeminiFreeApiClient`, etc.).
- Streaming handled via callbacks from `SseClient`.
- No centralized executor or coroutine usage; introducing `ExecutorService` or RxJava would reduce duplication and improve control.
- UI updates rely on `runOnUiThread` or `Handler` posted from managers.

## Error Handling & Logging
- Global crashes are caught by `CodeXApplication` and displayed in `DebugActivity` with stack traces passed via Intent extras.
- API errors call `AIAssistant.AIActionListener.onAiError()`; ensure listener is non-null before invocation.
- Logging mostly uses `Log.d`/`Log.e` inline; no structured logging.
- Toasts provide user feedback (e.g., missing cookies in `GeminiFreeApiClient`).

## Resources & Assets
- `assets/` contains prompt templates, stylesheets, and perhaps example projects (verify contents when extending).
- `res/drawable` includes icons for file types, actions, statuses.
- `res/values` hosts strings, themes (light/dark), and color schemes.
- `res/xml/file_paths.xml` defines `FileProvider` paths matching `AndroidManifest.xml` provider entry.

## Performance & Stability Notes
- Many classes exceed 500 lines (e.g., `AiAssistantManager`, `ChatMessageAdapter`, `PreviewActivity`, `QwenApiClient`, `GeminiFreeApiClient`). Split into cohesive modules to honor the project guideline and improve readability.
- Markdown rendering and bitmap decoding occur inside `RecyclerView` binders; cache results or pre-process off the UI thread.
- Frequent `new Thread().start()` calls may cause thread exhaustion; migrate to a shared executor.
- SSE parsing in `QwenApiClient` builds strings aggressively; consider streaming JSON parsing.
- `GeminiFreeApiClient` schedules background cookie refreshes; ensure `scheduler.shutdown()` on teardown to avoid leaks.

## Testing & Quality
- No instrumentation/unit tests currently in repo. Recommended additions:
  - Unit tests for `QwenResponseParser`, `ToolExecutor`, `DiffGenerator`.
  - Integration tests simulating AI chat flows with mock API clients.
  - UI tests for `EditorActivity` using Espresso.
- Enable Android Lint, PMD, or SpotBugs to detect unused code and complexity issues.
- Consider adding CI workflow (GitHub Actions) to run `./gradlew lint test` on PRs.

## Extending the Codebase

### Adding a New AI Provider
1. Create `YourProviderApiClient` implementing `ApiClient` in `app/src/main/java/com/codex/apk/`.
2. Add provider enum to `AIProvider` and default models in `AIModel` or persisted config.
3. Update `AIAssistant.initializeApiClients()` to register the new client.
4. Define provider-specific prompts or capabilities via `ModelCapabilities` if needed.
5. Update UI (`ModelsActivity`, `SettingsActivity`) to expose configuration.

### Introducing Modular Architecture
- Add AndroidX Lifecycle dependencies and migrate `EditorActivity` state into `EditorViewModel` (`editor/EditorViewModel.java`).
- Extract service layers (API, storage, diffing) behind interfaces. Consider dependency injection (Hilt or manual factory) to simplify unit testing.
- For Compose adoption, port `ChatMessageAdapter` UI to composables for declarative updates.

### Tooling Enhancements
- Extend `ToolExecutor` to support additional operations (e.g., new file creation templates) by updating `ToolSpec` definitions and handler logic.
- Enhance `PlanExecutor` to visualize progress via `ChatMessage.PlanStep.status` updates.

## Known Refactoring Targets
- `editor/AiAssistantManager.java` (~950 LOC).
- `ChatMessageAdapter.java` (606 LOC) – split by view types.
- `SimpleSoraTabAdapter.java`, `QwenApiClient.java`, `GeminiFreeApiClient.java`, `PreviewActivity.java`, `TemplateManager.java`, `ProjectManager.java` – all near or above threshold.
- Consolidate file operations between `FileManager`, `AdvancedFileManager`, and `FileOps`.
- Abstract duplicated OkHttp setup across API clients.

## Troubleshooting Guide
- **Missing AI Responses**: Verify credentials in `SettingsActivity`. For Qwen, ensure `QwenMidTokenManager.ensureMidToken()` succeeds (check logcat for `QwenMidTokenManager`).
- **File Actions Not Applying**: Confirm `ToolExecutor` has storage permissions. `PermissionManager` handles runtime prompts; watch for denial in logs.
- **Crash on Launch**: Inspect `DebugActivity` output. Common cause: missing theme resource referenced in `AndroidManifest.xml`.
- **Markdown Rendering Issues**: Check `MarkdownFormatter.getInstance()` initialization; ensure Markwon dependencies are synced.
- **Slow Scrolling in Chat**: Profile `ChatMessageAdapter.bind()`; consider precomputing Markdown and replacing heavy anonymous adapters for attachments.

## Contribution Guidelines
- Maintain files under 500 lines when feasible; split logic into focused classes.
- Follow existing Java code style (4-space indent, braces on new lines).
- Document public methods with concise comments; avoid removing existing documentation absent need.
- Run static analysis and format code before PRs.
- Coordinate large refactors to avoid disrupting concurrent contributors; consider feature branches per module.

## Glossary
- **Agent Mode**: When enabled, AI can produce action plans with tool invocations; toggled in `SettingsActivity`.
- **Thinking Mode**: Adds chain-of-thought style responses stored in `ChatMessage.getThinkingContent()`.
- **Plan Steps**: Structured actions produced by AI, rendered via plan cards in `ChatMessageAdapter`.
- **Tool Usage**: Metadata on executed tools displayed as chips in AI messages.

## Dependency Appendix
- `com.squareup.okhttp3:okhttp`: HTTP client for all API integrations; see `QwenApiClient` and `GeminiFreeApiClient` for usage patterns.
- `com.google.code.gson:gson`: JSON parsing building blocks (`JsonParser`, `JsonObject`) throughout API clients.
- `io.noties.markwon:*`: Markdown parsing and rendering; used in `MarkdownFormatter` and `ChatMessageAdapter`.
- `org.eclipse.jgit:org.eclipse.jgit`: Git commands in `GitManager` for commit/push/diff.
- `commons-io:commons-io`: Utility methods for file copying and stream handling in `FileOps`.
- `androidx.recyclerview:*`: Backbone for list presentations (chat, projects, tabs).
- `io.github.rosemoe:editor-*`: Advanced code editor component embedded in `CodeEditorFragment`.

---

This handbook provides future developers and AI agents with the essential context to navigate, extend, and maintain the Codex Android application. Pair this document with inline code comments for fine-grained implementation details.
