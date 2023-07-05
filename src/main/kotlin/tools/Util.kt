package tools

import java.io.InputStream

internal object Resources

fun resourceStream(name: String): InputStream {
    return Resources.javaClass.getResourceAsStream(name)!!
}

fun resourceByteArray(path: String): ByteArray {
    return resourceStream(path).readAllBytes()
}

fun ByteArray.toHex(): String = joinToString(separator = " ") { eachByte -> "%02x".format(eachByte) }
