# Platform Status

This prototype builds and runs on Android and desktop JVM (Windows, macOS, Linux).

Both targets share the Kotlin Compose UI, domain model, repository, and controller. Native platform adapters handle file storage, photo selection, navigation, and sharing.

iOS has a Swift host and iosArm64, iosSimulatorArm64, and iosX64 targets, enabled on macOS. It runs the same Compose AtlasApp, including the V3 notebook and restored V2 profile. UIKit/Photos/CoreLocation/URLSession adapters supply photos, documents, image saving/sharing, navigation, location and networking. See [iOS build instructions](iosApp/README.md) and the macOS GitHub Actions run for compile/test results. Device installation requires the owner's Apple signing identity.
