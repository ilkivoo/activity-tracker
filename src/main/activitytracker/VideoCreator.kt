package activitytracker

import activitytracker.converter.DiffMatchPatchConverter
import activitytracker.model.Diffs
import com.xuggle.mediatool.ToolFactory
import com.xuggle.xuggler.ICodec
import name.fraser.neil.plaintext.diff_match_patch
import java.awt.Color
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class VideoCreator(val curCode: String) {
    private val filePathForVideo = "/Users/alyokhina-o/srw3/src/main/resources/activity_tracker.mp4"
    private val imageCreator = ImageCreator()


    private val screenBounds = Toolkit.getDefaultToolkit().screenSize
    private val width = screenBounds.width
    private val height = screenBounds.width
    private val min5 = 300000

    fun create(diffs1: List<Diffs>) {
        val diffs = diffs1.stream()
                .sorted { o1, o2 -> (o1.timestamp - o2.timestamp).toInt() }
                .collect(Collectors.toList())
        val old = getStartCode(diffs, curCode)
        val textRedactor = TextRedactor(old)
        if (diffs.isEmpty()) {
            return
        }
        val writer = ToolFactory.makeWriter(filePathForVideo)
        val screenBounds = Toolkit.getDefaultToolkit().screenSize
        writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, screenBounds.width / 2, screenBounds.height / 2)
        val firstDiffOpt = diffs.stream().min { o1, o2 -> (o1.timestamp - o2.timestamp).toInt() }
        val minTs = if (firstDiffOpt.isPresent) firstDiffOpt.get().timestamp else diffs[0].timestamp
        for (i in 0 until diffs.size) {
            val diff = diffs[i]
            val proc = getMA(diffs, i)
            val text = textRedactor.getText(diff, width, height)
            var image = imageCreator.create(diff,
                    text,
                    BufferedImage.TYPE_3BYTE_BGR,
                    screenBounds.width,
                    screenBounds.height)
            val color = when {
                proc > 40 -> Color.RED
                proc < 20 -> Color.GREEN
                else -> Color.YELLOW
            }
            image = imageCreator.addRectangle(image, color, 0, 0, 20, 20)
            val ts = Math.round((diff.timestamp - minTs) / 2.0)
            writer.encodeVideo(0, image, ts, TimeUnit.MILLISECONDS)
        }
        writer.close()
    }

    private fun getMA(diffs: List<Diffs>, ind: Int): Long {
        var eventCount = 0
        var pasteCount = 0
        var curInd = ind
        val minTs = diffs[curInd].timestamp - min5

        while (curInd > 0) {
            if (diffs[curInd].timestamp < minTs) {
                break
            }
            eventCount++
            if (diffs[curInd].action.equals("EditorPaste")) {
                pasteCount++
            }
            curInd--
        }
        return Math.round((pasteCount * 100.0) / eventCount)
    }

    private fun getStartCode(diffs: List<Diffs>, newString: String): String {
        val dmp = diff_match_patch(null)
        var cur = newString
        for (i in diffs.size - 1 downTo 0) {
            val diffMatchPatch = DiffMatchPatchConverter.fromModelNew(diffs[i], cur)
            cur = dmp.diff_text1(LinkedList(diffMatchPatch))
        }
        return cur
    }
}