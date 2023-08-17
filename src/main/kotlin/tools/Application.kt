package tools

import org.slf4j.bridge.SLF4JBridgeHandler
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.ScopeType
import tools.gfx.*
import tools.mads.OBXReader
import kotlin.system.exitProcess

@Command(
    name = "tools",
    description = ["Atari Tools project"],
    mixinStandardHelpOptions = true,
    scope = ScopeType.INHERIT,
    subcommands = [
        Atl2Hex::class,
        DataToHex::class,
        CompressLZ::class,
        DecompressLZ::class,
        RGBToHex::class,
        ChrToPNG::class,
        PNGToChr::class,
        ChrToASM::class,
        OBXReader::class
    ]
)
open class Application : Runnable {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SLF4JBridgeHandler.removeHandlersForRootLogger()
            SLF4JBridgeHandler.install()
            exitProcess(CommandLine(Application()).execute(*args))
        }
    }

    override fun run() {}
}