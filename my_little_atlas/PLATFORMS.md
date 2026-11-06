# Platform Status

This prototype builds and runs on Android and desktop JVM (Windows, macOS, Linux).

Both targets share the Kotlin Compose UI, domain model, repository, and controller. Native platform adapters handle file storage, photo selection, navigation, and sharing.

iOS is not a build target in this draft. It needs a native host app and UIKit adapters for photos, document import/export, and sharing.
