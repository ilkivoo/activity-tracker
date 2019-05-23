package activitytracker.log

import activitytracker.model.Diff
import activitytracker.model.Diffs
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import com.opencsv.CSVWriter.*
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.TreeSet
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList

class Logger {
    companion object {
        private const val fileName = "/Users/alyokhina-o/activity-tracker/activity_tracker.csv"
        private val headers = arrayOf("diffs", "fileName", "timestamp", "action", "params")

        private val fileWriter: FileWriter
        private val csvWriter: CSVWriter
        private const val hour = 3600000
        private val records: TreeSet<Diffs> = TreeSet(Comparator { o1, o2 -> (o1.timestamp - o2.timestamp).toInt() })
        private val recordsLock = ReentrantLock()
        private val executorService = Executors.newScheduledThreadPool(1)
        private val objectMapper = ObjectMapper()


        init {
            val file = File(fileName)
            file.deleteOnExit()
            fileWriter = FileWriter(fileName)
            csvWriter = CSVWriter(fileWriter, DEFAULT_SEPARATOR, DEFAULT_QUOTE_CHARACTER, '\\', RFC4180_LINE_END)
            csvWriter.writeNext(headers)
            csvWriter.flushQuietly()
            fileWriter.flush()
            executorService.scheduleWithFixedDelay(this::save, 0, 10, TimeUnit.SECONDS)
        }

        fun save() {
            val rows = ArrayList<Array<String>>()
            recordsLock.lock()
            for (diff in records) {
                val row = arrayOf(
                        objectMapper.writeValueAsString(diff.diffs),
                        diff.fileName,
                        diff.timestamp.toString(),
                        diff.action ?: "",
                        objectMapper.writeValueAsString(diff.params))
                rows.add(row)
            }
            records.clear()
            recordsLock.unlock()
            csvWriter.writeAll(rows)
            csvWriter.flushQuietly()
            fileWriter.flush()
        }


        fun write(diffs: Diffs) {
            if (diffs.diffs.isEmpty()) {
                return
            }
            recordsLock.lock()
            records.add(diffs)
            recordsLock.unlock()
        }

        fun getLogs(): List<Diffs> {
            val fileReader = FileReader(fileName)
            val csvReader = CSVReader(fileReader)
            csvReader.readNext()//skip headers
            val currTs = System.currentTimeMillis()
            val minTs = currTs - hour
            val result = ArrayList<Diffs>()
            while (true) {
                val log = csvReader.readNext() ?: break
                try {
                    if (log[2].toLong() < minTs) {
                        break
                    }
                    val diff = objectMapper.readValue<List<Diff>>(log[0], object : TypeReference<List<Diff>>() {})
                    val params = objectMapper.readValue<Map<String, String>>(log[4], object : TypeReference<Map<String, String>>() {})
                    result.add(Diffs(diff, log[1], log[2].toLong(), log[3], params))
                } catch (ignore: Exception) {
                    println(log[0])
                    println(ignore.message)
                }
            }
            return result
        }
    }

}