package app.kleinmusic.adblock

/**
 * Declarações das funções escritas em Rust (native/adblock).
 * O arquivo libklein_adblock.so é gerado pelo GitHub Actions e vai para app/src/main/jniLibs.
 */
object NativeAdBlock {
    init {
        System.loadLibrary("klein_adblock")
    }

    external fun nativeCreate(rules: String): Long
    external fun nativeShouldBlock(handle: Long, url: String, sourceUrl: String, requestType: String): Boolean
    external fun nativeDestroy(handle: Long)
}
