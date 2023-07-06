package tools.gfx

import mu.KotlinLogging
import picocli.CommandLine
import tools.compress.Compressor
import java.io.File
import kotlin.math.abs

private val logger = KotlinLogging.logger {}

// This only works at the moment with 4 input colours in the RGB. If we relax the need to keep the order of colours,
// and only match black to colour 0, then it could work on any number of colours, but not sure on if they will map well when showing in atari.
// Instead, could order the colours by "power" and assign colour indexes from 0 (black) to brightest (max colours).
@CommandLine.Command(
    name = "rgb2hex",
    description = ["Convert an RGB multi-colour file to raw hex"]
)
class RGBToHex : Runnable {
    @CommandLine.Option(names = ["-d", "--data"], description = ["AtariTools 800 asm file"], required = true)
    var dataFileArg: File? = null

    @CommandLine.Option(names = ["-o", "--output"], description = ["HEX output file name (will have section name put in it, e.g. out.hex -> out-grapha.hex)"], required = true)
    var outputFileArg: File? = null

    @CommandLine.Option(names = ["-b", "--bpp"], description = ["Bits per pixel. Got from required mode, e.g. m8 = 1, m7 = 2, m11 = 4, m10 = 8. Must be one of 1,2,4,8"], required = true)
    var bitsPerPixel: Int = 0

    @CommandLine.Option(names = ["--compress"], description = ["Compress the data before saving. Default: false"], required = false, negatable = true, defaultValue = "false")
    var compress: Boolean = false

    override fun run() {
        if (bitsPerPixel != 2) {
            throw Exception("Sorry, only supporting 2 bits per pixel at the moment")
        }

        val dataFile = dataFileArg!!
        val outputFile = outputFileArg!!

        logger.info { "Reading DATA file: $dataFile" }

        // Each triple of bytes is an RGB colour. Scan for unique values.
        // Then assign each one a colour index, e.g. 0, 1, 2, 3 in a 4 colour image.
        // We need to then use the bitsPerPixel to decide how to construct the hex data from the RGB.
        // e.g. if it's 2 bits per pixel, a byte will have 4 pixels, and each 2 bits will represent the appropriate colour index.
        // so in binary: 11.01.00.10 would represent 4 pixels with colours 3,1,0,2, and the byte to output would be $d2 (210)

        val data = dataFile.readBytes()
        if (data.size % 3 != 0) {
            logger.error { "File size is not multiple of 3. Aborting trying to read RGB values" }
            return
        }

        val windowedData = data.toList().windowed(3, 3).map { wb -> wb.joinToString("") { String.format("%02X", it) } }
        val uniqueValues = windowedData.toSet()
        logger.info { "Found ${uniqueValues.count()} values: $uniqueValues" }

        val hexToColour = uniqueValues.associateWith { determineClosestColor(it) }
        if (hexToColour.values.toSet().size != hexToColour.values.size) {
            throw Exception("Multiple colours in the image map to same colour: $hexToColour")
        }

        // now convert the RGB from original value into a colour index
        val colours = windowedData.map { hexToColour.getOrElse(it) { throw Exception("Didn't find mapping 2nd time for $it") } }

        // group them so each list fits in a byte (window size = 8 / bits per pixel)
        val colourWindows = colours.windowed(8 / bitsPerPixel, 8 / bitsPerPixel)
        val bytes = colourWindows.fold(mutableListOf<Byte>()) { acc, cs -> // list of 1,2,4 or 8 colours to pack
            when (cs.size) {
                1 -> {
                    // 8 bits per pixel, 1 pixel per byte
                    acc += cs.first().index.toByte()
                }
                2 -> {
                    // 4 bits per pixel, 2 pixels per byte
                    val b1 = cs[0].index * 16 // shifted into top 4 bits
                    val b0 = cs[1].index
                    acc += (b1 + b0).toByte()
                }
                4 -> {
                    // 2 bits per pixel, 4 pixels per byte
                    val b3 = cs[0].index * 64 // shift up 6 bits
                    val b2 = cs[1].index * 16 // shift up 4 bits
                    val b1 = cs[2].index * 4  // shift up 2 bits
                    val b0 = cs[3].index
                    acc += (b3 + b2 + b1 + b0).toByte()
                }
                8 -> {
                    // 1 bit per pixel (2 colours), 8 pixels per byte
                    throw Exception("Not implemented yet")
                }
            }
            acc
        }

        val toSave = if (compress) Compressor().compress(bytes) else bytes
        if (compress) logger.info { "Compressed ${bytes.size} to ${toSave.size}" }
        logger.info { "Writing raw data to $outputFile" }
        outputFile.writeBytes(toSave.toByteArray())
    }

    enum class Color(val index: Int) {
        RED(1), BLUE(3), YELLOW(2), BLACK(0)
    }

    private fun determineClosestColor(hexString: String): Color {
        // These are roughly the RGB values of 4 colours used in GrafX2 (according to GIMP).
        // They then are transformed using graphicsmagick to RGB data using:
        // $  gm convert image.gif -depth 4 RGB:out.raw
        // which alters them to something like FF3333 (red), CCBB55 (yellow), 551199 (blue).
        // and we want to preserve the colour indexes from original values (do we?)

        val customRed = "ff3935"
        val customBlue = "6117a3"
        val customYellow = "d3c55f"
        val customBlack = "000000"

        val redDistance = calculateDistance(hexString, customRed)
        val blueDistance = calculateDistance(hexString, customBlue)
        val yellowDistance = calculateDistance(hexString, customYellow)
        val blackDistance = calculateDistance(hexString, customBlack)

        return when (minOf(redDistance, blueDistance, yellowDistance, blackDistance)) {
            redDistance -> Color.RED
            blueDistance -> Color.BLUE
            yellowDistance -> Color.YELLOW
            else -> Color.BLACK
        }
    }

    private fun calculateDistance(color1: String, color2: String): Int {
        val r1 = color1.substring(0, 2).toInt(16)
        val g1 = color1.substring(2, 4).toInt(16)
        val b1 = color1.substring(4, 6).toInt(16)

        val r2 = color2.substring(0, 2).toInt(16)
        val g2 = color2.substring(2, 4).toInt(16)
        val b2 = color2.substring(4, 6).toInt(16)

        val rDistance = abs(r1 - r2)
        val gDistance = abs(g1 - g2)
        val bDistance = abs(b1 - b2)

        return rDistance + gDistance + bDistance
    }
}