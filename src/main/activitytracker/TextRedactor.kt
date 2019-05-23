package activitytracker

import activitytracker.converter.DiffMatchPatchConverter
import activitytracker.model.Diffs
import name.fraser.neil.plaintext.diff_match_patch
import java.util.*
import kotlin.collections.ArrayList

class TextRedactor(baseString: String) {
    private var curCode = baseString
    private val dmp = diff_match_patch(null)
    private val maxTextWidth = 120
    private val countLines = 30
    private val importRegexp = "import .*?".toRegex()


    fun getText(diff: Diffs, width: Int, height: Int): List<String> {
        val curText = getNewString(curCode, diff)
        curCode = curText
        val line = (diff.params["MOUSE_COLUMN"] ?: "0").toInt()
        val pos = (diff.params["MOUSE_LINE"] ?: "0").toInt()
        val font = diff.params["ParamsType.FONT_SIZE"]?.toInt() ?: 14
        return cutAndDeleteImports(curText, line, pos, countLines)
    }

    private fun cutAndDeleteImports(text: String, line: Int, pos: Int, count: Int): List<String> {
        val lines = text.split(System.lineSeparator())
        val inds = TreeSet<Int>()
        var l = line
        var r = line
        var isExistImport = false
        inds.add(line)
        while (inds.size < count) {
            if (l - 1 < 0 && r + 1 > lines.size - 1) {
                break
            }
            if (l - 1 >= 0) {
                if (importRegexp.containsMatchIn(lines[l - 1])) {
                    isExistImport = true
                } else {
                    inds.add(l - 1)
                }
                l -= 1
            }
            if (r + 1 <= lines.size - 1) {
                if (importRegexp.containsMatchIn(lines[r + 1])) {
                    isExistImport = true
                } else {
                    inds.add(r + 1)
                }
                r += 1
            }
        }
        val result = ArrayList<String>()
        if (isExistImport) {
            result.add("import ...")
        }
        for (i in inds) {
            result.add(lines[i].substring(0, minOf(maxTextWidth, lines[i].length)))
        }
        return result
    }

    private fun getNewString(old: String, diffs: Diffs): String {
        try {
            val diffMatchPatch = DiffMatchPatchConverter.fromModelOld(diffs, old)
            val new = dmp.diff_text2(LinkedList(diffMatchPatch))
            return new
        } catch (e: Exception) {
            println(e.message)
        }
        return ""
    }


}