package apps.chocolatecakecodes.cotemplate.util

internal inline infix fun <T, R> T.Let(block: (T) -> R): R {
    return block(this)
}
