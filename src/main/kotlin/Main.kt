import org.apache.commons.lang3.RandomStringUtils
import java.io.BufferedWriter
import java.io.File

const val MAX_BLOCK_SIZE = 200000
const val PAIR_SIZE = 2
const val MB_DIVIDER = 1024 * 1024

const val WORK_DIRECTORY = "./"
const val SOURCE_FILE_NAME = "source"
const val RESULT_FILE_NAME = "result"
const val EXTENSION = ".txt"
const val TEMP_PREFIX = "temp-"

const val SOURCE_FILE_PATH = "$WORK_DIRECTORY$SOURCE_FILE_NAME$EXTENSION"

fun main() {
    printMemoryStatistics()

    println("Start file generation")
    val startGenerationTime = System.currentTimeMillis()
    val sourceFile = generateFile(1000000, 600)
    println("File has successfully generated in ${System.currentTimeMillis() - startGenerationTime} milliseconds")
    println("File size: ${sourceFile.length() / MB_DIVIDER} mb")

    println("Start file sorting")
    val startSortingTime = System.currentTimeMillis()
    sortFile(sourceFile)
    println("File has successfully sorted in ${System.currentTimeMillis() - startSortingTime} milliseconds")

    printMemoryStatistics()
}

fun sortFile(file: File) {
    var filesCount = partitionFile(file)

    while (filesCount > 1) {
        val pairs = (1..filesCount)
            .toList()
            .chunked(PAIR_SIZE)

        pairs.forEachIndexed { index, pair ->
            if (pair.size == PAIR_SIZE) {
                sortPair(pair[0], pair[1], index + 1)
            } else {
                saveFileForNextIteration(pair[0], index + 1)
            }
        }
        filesCount = pairs.size
    }
    renameFile("$filesCount", RESULT_FILE_NAME)
}

private fun generateFile(strCount: Int, maxStrLength: Int): File {
    val myFile = File(SOURCE_FILE_PATH)
    myFile.bufferedWriter().use { out ->
        repeat(strCount) {
            out.write("${RandomStringUtils.randomAlphabetic(1, maxStrLength)}\n")
        }
    }
    return myFile
}

private fun partitionFile(file: File): Int {
    var filesCount = 0
    file.bufferedReader().use { br ->
        val it = br.lineSequence().iterator()
        val block = mutableListOf<String>()
        while (it.hasNext()) {
            block.add(it.next())
            if (block.size == MAX_BLOCK_SIZE) {
                filesCount++
                writeBlockToFile(filesCount, block)
                block.clear()
            }
        }
        if (block.size > 0) {
            writeBlockToFile(filesCount + 1, block)
            filesCount++
        }
    }
    return filesCount
}

private fun sortPair(fileName1: Int, fileName2: Int, destination: Int) {
    val file1 = File("$WORK_DIRECTORY$fileName1$EXTENSION")
    val file2 = File("$WORK_DIRECTORY$fileName2$EXTENSION")
    val destinationFile = File("$WORK_DIRECTORY$TEMP_PREFIX$destination$EXTENSION")
    val destinationBw = destinationFile.bufferedWriter()

    destinationBw.use { out ->
        file1.bufferedReader().use { br1 ->
            file2.bufferedReader().use { br2 ->
                val it1 = br1.lineSequence().iterator()
                val it2 = br2.lineSequence().iterator()

                var buf = it1.next()
                var isFirstFileBuf = true

                while (it1.hasNext() && it2.hasNext()) {
                    val line = if (isFirstFileBuf) it2.next() else it1.next()
                    if (line < buf) {
                        out.write("$line\n")
                    } else {
                        out.write("$buf\n")
                        buf = line
                        isFirstFileBuf = !isFirstFileBuf
                    }
                }

                if (it1.hasNext()) {
                    addRemaining(it1, buf, out)
                } else if (it2.hasNext()) {
                    addRemaining(it2, buf, out)
                }
            }
        }
    }

    file1.delete()
    file2.delete()
    destinationFile.renameTo(File("$WORK_DIRECTORY$destination$EXTENSION"))
}


private fun addRemaining(iterator: Iterator<String>, bufInitial: String, out: BufferedWriter) {
    var buf = bufInitial
    while (iterator.hasNext()) {
        val line = iterator.next()
        if (line < buf) {
            out.write("$line\n")
        } else {
            out.write("$buf\n")
            buf = line
        }
    }
    out.write("$buf\n")
}

private fun writeBlockToFile(fileIndex: Int, block: MutableList<String>) {
    val path = "$WORK_DIRECTORY$fileIndex$EXTENSION"
    File(path).bufferedWriter().use { out ->
        block.sorted().forEach {
            out.write("$it\n")
        }
    }
}

private fun saveFileForNextIteration(fileIndex: Int, nextIterationFileIndex: Int) {
    renameFile("$fileIndex", "$nextIterationFileIndex")
}

private fun renameFile(oldName: String, newName: String) {
    File("$WORK_DIRECTORY$oldName$EXTENSION").renameTo(File("$WORK_DIRECTORY$newName$EXTENSION"))
}

private fun printMemoryStatistics() {
    println("JVM memory: free = ${getFreeMemory()} mb, total = ${getTotalMemory()} mb, max = ${getMaxMemory()} mb")
}

private fun getFreeMemory() =
    Runtime.getRuntime().freeMemory() / MB_DIVIDER

private fun getTotalMemory() =
    Runtime.getRuntime().totalMemory() / MB_DIVIDER

private fun getMaxMemory() =
    Runtime.getRuntime().maxMemory() / MB_DIVIDER
