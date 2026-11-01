# Nexora Matrix SDK integration

Nexora now declares the official Matrix Rust SDK Android artifact:

`org.matrix.rustcomponents:sdk-android:26.08.05`

This is the official Matrix Rust SDK Android binding distribution, not a custom cryptography implementation. The project also enables Kotlin for the Android module so the Kotlin FFI can be integrated.

**Important:** Adding the SDK dependency alone does not activate E2EE. The current Java API client remains the active chat transport until the app is migrated to the Rust SDK session/client/timeline APIs. Do not assume private chats are encrypted yet. The next integration work must replace direct REST login/sync/send with Rust SDK session handling and validate device verification, cross-signing, recovery, encrypted room events and encrypted attachments before enabling encrypted rooms by default.

Source: https://github.com/matrix-org/matrix-rust-components-kotlin
