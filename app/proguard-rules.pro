# R8 keep rules for Resume Builder release builds.
# Library consumer rules (Room, kotlinx.serialization, Koin, Compose, navigation) are applied automatically.

# PDFBox-Android references the optional JP2 (JPEG 2000) decoder, which isn't bundled.
# Text extraction for resume import doesn't need it; only JPX-encoded images in a PDF would fail to decode.
-dontwarn com.gemalto.jp2.**
