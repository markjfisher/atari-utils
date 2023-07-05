package tools.gfx

import mu.KotlinLogging
import picocli.CommandLine
import tools.compress.Compressor
import java.io.File
import java.io.UnsupportedEncodingException

/*
// Converts the AtariTools-800 ASM output into binary data files, which can be used with "INS" MADS instruction.
// Saves all data in every section to a single file. It's up to the user to split it at boundary level when using it.
// Typically, these files will also be compressed into LZ style format with "compress" tool

// Input file format is:
;
;     >>> ATARITOOLS-800 GRAPHIC 160X192X4C DATA & SAMPLE CODE FOR MADS ASM/ALTIRRA WUDSN IDE WORKCHAIN
;

GRAPHA  ; DATA FOR THE GRAPHIC-SCREEN (PART A ZONE)
        .byte $00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00
        //...
        .byte $00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$17,$EA,$AA,$FF,$AA,$AA,$AA,$AF,$F3,$EA,$AA,$AA,$AA,$FE,$AA,$AB,$90,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00

GRAPHB  ; DATA FOR THE GRAPHIC-SCREEN (PART B ZONE)

        .byte $00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$24,$EA,$AA,$FF,$AA,$AA,$AA,$AF,$FF,$FA,$AA,$AA,$AA,$FF,$AA,$AB,$90,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00
        // ...
        .byte $00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00,$00

Color_data   ; 712 (0 AND BORDER), 708(1), 709(2), 710(3)

        .byte $02,$C6,$34,$00
*/

private val logger = KotlinLogging.logger {}
private val pass: Unit = Unit

// tt data2hex -d fn320x24.data -o fn320x24.hex

@CommandLine.Command(
    name = "atl2hex",
    description = ["Convert AtariTools 800 ASM to raw hex to be included in asm files"]
)
class Atl2Hex : Runnable {
    @CommandLine.Option(names = ["-d", "--data"], description = ["AtariTools 800 asm file"], required = true)
    var dataFileArg: File? = null

    @CommandLine.Option(names = ["-o", "--output"], description = ["HEX output file name (will have section name put in it, e.g. out.hex -> out-grapha.hex)"], required = true)
    var outputFileArg: File? = null

    @CommandLine.Option(names = ["-r", "--rows"], description = ["Row count to output, default: 0 (which means all)"], required = false)
    var rows: Int = 0

    @CommandLine.Option(names = ["-c", "--compress"], description = ["Compress the data before saving. Default: false"], required = false)
    var compress: Boolean = false

    override fun run() {
        val dataFile = dataFileArg!!
        val outputFile = outputFileArg!!

        logger.info { "Reading DATA file: $dataFile" }
        // get the line containing "ATARITOOLS-800" and read the mode
        val lines = dataFile.readLines()
        val screenInfo = lines.drop(1).take(1).first().split("\\s+".toRegex())[4].split("X")

        lateinit var gfxMode: GfxMode

        val simpleMode = mapOf(
            8 to GfxMode("2C", 320, 192),
            9 to GfxMode("16S", 80, 192),
            10 to GfxMode("9C", 80, 192),
            11 to GfxMode("16H", 80, 192)
        )
        gfxMode = when (screenInfo.size) {
            3 -> GfxMode(screenInfo[2], screenInfo[0].toInt(), screenInfo[1].toInt())
            1 -> simpleMode[screenInfo[0].toInt()] ?: throw Exception("Unknown mode: $screenInfo")
            else -> throw UnsupportedEncodingException("I don't yet understand how to decode '$screenInfo'")
        }
        logger.info {
            """

             Graphics Mode: ${gfxMode.mode}
            Bytes Per Line: ${gfxMode.bytesPerRow}
                    Height: ${gfxMode.height}
        """.trimIndent()
        }

        // anything that starts with text (except semicolon) is a definition of a new block of gfx
        // ends when new section name is Color_data

        val allData: MutableList<Byte> = mutableListOf()
        var processing = true
        var lineIndex = 0
        while (processing) {
            val currentLine = lines[lineIndex++]
            when {
                currentLine.startsWith(";") || currentLine.trim().isEmpty() -> pass
                currentLine.startsWith("GRAPH") -> {
                    val currentSection = currentLine.split("\\s+".toRegex())[0]
                    println("Reading new section: $currentSection")
                }

                currentLine.startsWith("Color_data") -> processing = false
                else -> {
                    // we have a line of "   .byte $AA,$BB, ...", add it as bytes to current section
                    val hexes = currentLine.trim().split("\\s+".toRegex())[1].split(",").map { it.removePrefix("\$").toInt(16).toByte() }
                    allData += hexes
                }
            }
        }

        val numBytesToSave = gfxMode.bytesPerRow * rows  // 0 means all
        val data = if (numBytesToSave > 0) allData.take(numBytesToSave) else allData
        val toSave = if (compress) Compressor().compress(data) else data
        if (compress) logger.info { "Compressed ${data.size} to ${toSave.size}" }
        logger.info { "Writing raw data to $outputFile" }
        outputFile.writeBytes(toSave.toByteArray())
    }

    data class GfxMode(val mode: String, val bytesPerRow: Int, val height: Int)
}