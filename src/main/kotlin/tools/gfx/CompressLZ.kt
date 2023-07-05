package tools.gfx

import mu.KotlinLogging
import picocli.CommandLine
import tools.compress.Compressor
import java.io.File

// Compression algorithm taken from http://www.retrosoftware.co.uk/forum/viewtopic.php?f=73&t=999
// and converted to kotlin.

private val logger = KotlinLogging.logger {}

@CommandLine.Command(
    name = "compress",
    description = ["Compresses file using custom routine the atari can easily decompress"]
)
class CompressLZ : Runnable {
    @CommandLine.Option(names = ["-d", "--data"], description = ["AtariTools 800 asm file"], required = true)
    var dataFileArg: File? = null

    @CommandLine.Option(names = ["-o", "--output"], description = ["HEX output file name (will have section name put in it, e.g. out.hex -> out-grapha.hex)"], required = true)
    var outputFileArg: File? = null

    override fun run() {
        val dataFile = dataFileArg!!
        val outputFile = outputFileArg!!

        val input = dataFile.readBytes()
        val compressed = Compressor().compress(input.toList())
        logger.info { "Compressed ${input.size} to ${compressed.size}" }
        logger.info { "Writing to $outputFile" }
        outputFile.writeBytes(compressed.toByteArray())
    }
}