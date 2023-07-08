package tools.gfx

import mu.KotlinLogging
import org.w3c.dom.css.RGBColor
import picocli.CommandLine
import tools.compress.Compressor
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Math.pow
import javax.imageio.ImageIO
import kotlin.experimental.and
import kotlin.math.abs
import kotlin.math.pow

private val logger = KotlinLogging.logger {}

@CommandLine.Command(
    name = "chr2png",
    description = ["Convert raw character set to png for editing"]
)
class ChrToPNG : Runnable {
    @CommandLine.Option(names = ["-d", "--data"], description = ["1024 byte font memory dump"], required = true)
    var dataFileArg: File? = null

    @CommandLine.Option(names = ["-o", "--output"], description = ["PNG output file (Black/White pixels) representing fonts"], required = true)
    var outputFileArg: File? = null

    override fun run() {
        val dataFile = dataFileArg!!
        val outputFile = outputFileArg!!

        val bytes = dataFile.readBytes()

        // Read 1024 bytes, reading in blocks of 8 bytes per character.
        // First character is top left of a 256x32 grid, writing down 8 rows.
        val img = BufferedImage(256, 32, BufferedImage.TYPE_BYTE_BINARY)
        val onColour = Color.WHITE.rgb
        val offColour = Color.BLACK.rgb

        // split into a list of lists of 8 bytes for each char. list will have 128 entries.
        val by8 = bytes.toList().windowed(8, 8)

        // over all characters...
        by8.forEachIndexed { i, l8 ->
            val xTop = (i % 32) * 8
            val yTop = (i / 32) * 8
            // draw the 8 bytes into the image for this character
            l8.forEachIndexed { j, current ->
                for (b in 7 downTo 0) {
                    val p = current and 2.0.pow(b.toDouble()).toInt().toByte()
                    val xp = xTop + (7 - b)
                    val yp = yTop + j
                    img.setRGB(xp, yp, if (p.toInt() == 0) offColour else onColour)
                }
            }
        }

        logger.info { "Writing png to $outputFile" }
        ImageIO.write(img, "png", outputFile)
    }
}