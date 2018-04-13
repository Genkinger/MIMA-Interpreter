import java.io.*
import kotlin.system.exitProcess

class MimaVonNeumann(var stepping: Boolean) {
    private val commands = mapOf(
            "LDC" to 0b0001,
            "LDV" to 0b0010,
            "STV" to 0b0011,
            "LDIV" to 0b0100,
            "STIV" to 0b0101,
            "ADD" to 0b0110,
            "AND" to 0b0111,
            "NOT" to 0b1000,
            "OR" to 0b1001,
            "EQL" to 0b1010,
            "JMP" to 0b1011,
            "JMN" to 0b1100,
            "HALT" to 0b1101,
            "PUT" to 0b1111
    )

    private val revCommands = commands.entries.associateBy({ it.value }, { it.key })
    var halted = false
    var code = mutableListOf<String>()
    var memory: Array<Int> = Array(512, { _ -> 0 })
    var labels = mutableMapOf<String, Int>()
    var errorflag = false

    //--registers--
    var acc = 0
    var ip = 0


    fun loadFile(file: String) {
        File(file).bufferedReader().useLines {
            var line = 0
            it.forEach {

                var str = it
                if (!checkLine(it, line)) {
                    errorflag = true
                    println("Invalid keyword at Line $line")
                    str = "$it << Error: invalid keyword"
                } else {
                    str = it
                }
                code.add(str)
                line++
            }
        }
    }

    private fun checkLine(line: String, index: Int): Boolean {
        val parts = line.split(Regex("""\s+"""))
        when (parts.size) {
            1 -> return commands.containsKey(parts[0])
            2 -> {
                return if (commands.containsKey(parts[0])) {
                    true
                } else {
                    return if (commands.containsKey(parts[1])) {
                        labels[parts[0]] = index
                        true
                    } else {
                        false
                    }
                }
            }
            3 -> {
                labels[parts[0]] = index - 1
                return commands.containsKey(parts[1])
            }
        }
        return false
    }

    fun loadCode() {
        if (errorflag) {
            println("There are issues with the code...")
            code.forEach {
                println(it)
            }
            exitProcess(-1)
        }

        var line = 0
        code.forEach {

            val parts = it.trim().split(Regex("""\s+"""))


            when (parts.size) {
                1 -> {
                    memory[line] = commands[parts[0]]!!.toInt().toBinaryString().toInt(2)
                    line++
                }

                2 -> {
                    if (parts[1].toIntOrNull() == null) {
                        if (!commands.containsKey(parts[0])) {
                            memory[line] = commands[parts[1]]!!.toInt().toBinaryString().toInt(2)
                        } else {
                            memory[line] = commands[parts[0]]!!.toInt().toBinaryString().toInt(2) or labels[parts[1]]!!.toInt()
                        }
                    } else {
                        memory[line] = commands[parts[0]]!!.toInt().toBinaryString().toInt(2) or parts[1].toInt()
                    }
                    line++
                }

                3 -> {
                    //LBL JMP LBL
                    if (parts[2].toIntOrNull() == null) {
                        memory[line] = commands[parts[1]]!!.toInt().toBinaryString().toInt(2) or labels[parts[2]]!!.toInt()
                        //LBL CMD INPUT
                    } else {
                        memory[line] = commands[parts[1]]!!.toInt().toBinaryString().toInt(2) or parts[2].toInt()
                    }
                    line++
                }
            }
        }
    }

    fun executeCode() {
        while (!halted && !errorflag) {
            if (stepping) {
                println(">> Executing: ${code[ip]}")
                val input = readLine()
                when (input) {
                    "d", "dbg", "debug" -> debug()
                    "r", "reg", "register", "registers" -> registers()
                    "q", "quit", "exit" -> exitProcess(0)
                    else -> {
                        val bool = input!!.matches(Regex("""x [0-9]{1,3}"""))

                        if (bool) {
                            val parts = input.split(" ")
                            println("${parts[1]} => ${memory[parts[1].toInt()]}")
                        } else {
                            execute(memory[ip])
                            ip++
                        }
                    }
                }
            } else {
                execute(memory[ip])
                ip++
            }
        }
    }

    private fun execute(cmd: Int) {

        var code = cmd and ("1111" + "0".repeat(27)).toInt(2)
        code = code.shr(27)
        val data = cmd and ("0000" + "1".repeat(27)).toInt(2)

        when (revCommands[code]) {
            "HALT" -> halted = true
            "LDC" -> acc = data
            "LDV" -> acc = memory[data]
            "STV" -> memory[data] = acc
            "LDIV" -> acc = memory[memory[data]]
            "STIV" -> memory[memory[data]] = acc
            "ADD" -> acc += memory[data]
            "NOT" -> acc = acc.inv()
            "AND" -> acc = acc and memory[data]
            "OR" -> acc = acc or memory[data]
            "EQL" -> acc = if (acc == memory[data]) -1 else 0
            "JMP" -> ip = data
            "JMN" -> ip = if (acc == -1) data else ip
            "PUT" -> println("Memory at $data = ${memory[data]}")
        }
    }

    private fun registers() {
        println("ACC: $acc")
        println("IP: $ip")
    }

    fun debug() {
        registers()
        println("MEMORY:\n${memory.toBinaryString()}")
    }
}

fun printUsageAndExit(){
    println("Usage: java -jar mima.jar <input> [-d | -s]\n" +
            "\t-d: rudimentary stepping functionality\n" +
            "\t-s: summary at the end of execution (use this for a complete memory view)")
    exitProcess(-1)
}

/**
 * Extensions
 */
fun Array<Int>.toBinaryString(): String {
    var string = ""
    var index = 0
    this.forEach {
        string += "$index => ${"0".repeat(31 - it.toString(2).length) + it.toString(2)}\n"
        index++
    }
    return string
}
fun Int.toBinaryString(): String {
    var v = this.toString(2)
    val c = 4 - v.length
    v = "0".repeat(c) + v
    val cnt = 31 - v.length
    v += "0".repeat(cnt)
    return v
}

/**
 * Entry
 */
fun main(args: Array<String>) {

    when (args.size) {
        1 -> {
            with(MimaVonNeumann(false)) {
                loadFile(args[0])
                loadCode()
                executeCode()
            }
        }
        2 -> {
            when (args[1]) {
                "-d" -> {
                    with(MimaVonNeumann(true)) {
                        loadFile(args[0])
                        loadCode()
                        executeCode()
                    }
                }
                "-s" -> {
                    with(MimaVonNeumann(false)) {
                        loadFile(args[0])
                        loadCode()
                        executeCode()
                        debug()
                    }
                }
                else -> printUsageAndExit()
            }
        }
        3 -> {
            if (args[1] == "-d" && args[2] == "-s" || args[1] == "-s" && args[2] == "-d") {
                with(MimaVonNeumann(true)) {
                    loadFile(args[0])
                    loadCode()
                    executeCode()
                    debug()
                }
            } else {
                printUsageAndExit()
            }
        }
        else -> printUsageAndExit()
    }
}
