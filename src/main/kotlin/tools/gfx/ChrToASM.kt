package tools.gfx

import mu.KotlinLogging
import picocli.CommandLine
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.experimental.and
import kotlin.math.pow

private val logger = KotlinLogging.logger {}

@CommandLine.Command(
    name = "chr2asm",
    description = ["Convert raw character set to CA65 style .byte output"]
)
class ChrToASM : Runnable {
    @CommandLine.Option(names = ["-d", "--data"], description = ["1024 byte font memory dump"], required = true)
    var dataFileArg: File? = null

    @CommandLine.Option(names = ["-o", "--output"], description = ["PNG output file (Black/White pixels) representing fonts"], required = true)
    var outputFileArg: File? = null

    override fun run() {
        val dataFile = dataFileArg!!
        val outputFile = outputFileArg!!
        outputFile.writeText("")

        val bytes = dataFile.readBytes()

        // Read 1024 bytes, reading in blocks of 8 bytes per character.
        // First character is top left of a 256x32 grid, writing down 8 rows.

        // split into a list of lists of 8 bytes for each char. list will have 128 entries.
        val by8 = bytes.toList().windowed(8, 8)

        // over all characters...
        by8.forEachIndexed { i, l8 ->
            // output a line for this char
            outputFile.appendText("char_${i.toString(16).padStart(2, '0')}: .byte ")
            val hexString = l8.joinToString(", ") { "\$" + it.toUByte().toString(16).padStart(2, '0') }
            outputFile.appendText("$hexString\n")
        }

        logger.info { "Wrote output to $outputFile" }
    }
}