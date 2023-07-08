package tools.gfx

import mu.KotlinLogging
import picocli.CommandLine
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Math.pow
import javax.imageio.ImageIO
import kotlin.experimental.and
import kotlin.math.pow

private val logger = KotlinLogging.logger {}

@CommandLine.Command(
    name = "png2chr",
    description = ["Convert b/w png of character set to raw data for atari"]
)
class PNGToChr : Runnable {
    @CommandLine.Option(names = ["-d", "--data"], description = ["png file to convert to raw character data"], required = true)
    var dataFileArg: File? = null

    @CommandLine.Option(names = ["-o", "--output"], description = ["raw memory data output"], required = true)
    var outputFileArg: File? = null

    override fun run() {
        val dataFile = dataFileArg!!
        val outputFile = outputFileArg!!

        val png = ImageIO.read(dataFile)
        // read blocks of characters from the image pushing to an array
        val bytes = (0 until 128).fold(mutableListOf<Byte>()) { acc, ci ->
            val xC = ci % 32
            val yC = ci / 32
            for (l in 0 until 8) {
                var v = 0
                for (b in 7 downTo 0) {
                    val p = png.getRGB(xC * 8 + (7 - b), yC * 8 + l)
                    if (p == Color.WHITE.rgb) v += 2.0.pow(b).toInt()
                }
                acc += v.toByte()
            }
            acc
        }

        logger.info { "Writing chr data to $outputFile" }
        outputFile.writeBytes(bytes.toByteArray())

    }
}