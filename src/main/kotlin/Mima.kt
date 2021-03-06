import java.io.*
import kotlin.system.exitProcess
import com.xenomachina.argparser.*

class Arguments(parser: ArgParser){
	val debug by parser.flagging("-d","--debug",help = "enable rudimentary debugging")
	val summary by parser.flagging("-s","--summary",help = "enable summary")
	val memsize by parser.storing("-m","--memsize",help = "size of the memory"){ toInt() }
	val inputfile by parser.positional("SOURCE",help = "the input file to parse")
}

class MimaVonNeumann(val debug: Boolean, val summary: Boolean,val memsize: Int) {
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
    var memory: Array<Int> = Array(memsize, { _ -> 0 })
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
                        labels[parts[0]] = index - 1
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
                    memory[line] = commands[parts[0]]!!.toInt().shl(28)
                    line++
                }

                2 -> {
                    if (parts[1].toIntOrNull() == null) {
                        if (!commands.containsKey(parts[0])) {
                            memory[line] = commands[parts[1]]!!.toInt().shl(28)
                        } else {
                            memory[line] = commands[parts[0]]!!.toInt().shl(28) or labels[parts[1]]!!.toInt()
                        }
                    } else {
                        memory[line] = commands[parts[0]]!!.toInt().shl(28) or parts[1].toInt()
                    }
                    line++
                }

                3 -> {
                    if (parts[2].toIntOrNull() == null) {
                        memory[line] = commands[parts[1]]!!.toInt().shl(28) or labels[parts[2]]!!.toInt()
                    } else {
                            memory[line] = commands[parts[1]]!!.toInt().shl(28) or parts[2].toInt()
                    }
                    line++
                }
            }
        }
    }

    fun executeCode() {
        while (!halted && !errorflag) {
            if (debug) {
                println(">> Executing: ${code[ip]}")
                val input = readLine()
                when (input) {
                    "d", "dbg", "debug" -> summary()
                    "r", "reg", "register", "registers" -> registers()
                    "q", "quit", "exit" -> exitProcess(0)
                    else -> {
                        val bool = input!!.matches(Regex("""x [0-9]"""))

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

        val code = cmd.ushr(28)
        val data = cmd and (-1).ushr(4)

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
            else -> {
                error("invalid command")
            }
        }
    }

    fun begin(infile: String){
	    loadFile(infile)
	    loadCode()
	    executeCode()
	    if(summary){
		    summary()
	    }
    }
    
    private fun registers() {
        println("ACC: $acc")
        println("IP: $ip")
    }

    fun summary() {
        registers()
        println("MEMORY:\n${memory.toHexString()}")
    }
}

/**
 * Extensions
 */
fun Array<Int>.toHexString(): String {
    var string = ""
    var index = 0
    this.forEach {
        string += " $index => ${"0x" + Integer.toHexString(it)}\n"
        index++
    }
    return string
}


/**
 * Entry
 */
fun main(args: Array<String>) = mainBody {
	ArgParser(args).parseInto(::Arguments).run{
		with(MimaVonNeumann(debug,summary,memsize)){
			begin(inputfile)
		}
	}
}


