package activitytracker

import activitytracker.model.Diffs
import activitytracker.model.ParamsType
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ImageCreator {
    private val offY = 3
    private val offX = 3
    fun create(diff: Diffs,
               lines: List<String>,
               imageType: Int,
               width: Int,
               height: Int): BufferedImage {
        try {
            val img = UIUtil.createImage(width, height, imageType)
            addRectangle(img, Color.BLACK, 0, 0, width, height)

            val textStyle = diff.params["FONT_NAME"] ?: "Arial"
            val textSize = diff.params["ParamsType.FONT_SIZE"]?.toInt() ?: 14
            val font = Font(textStyle, Font.PLAIN, 24)

            val g2d = img.createGraphics()
            g2d.font = font
            val fm = g2d.fontMetrics
            g2d.color = Color.WHITE

            for (i in 0 until lines.size) {
                g2d.drawString(lines[i], offX * fm.ascent, offY * fm.ascent + (i + 1) * fm.ascent)
            }
            g2d.dispose()
            return img
        } catch (e: Exception) {
            println(e.message)
            throw e
        }
    }

    fun addRectangle(image: BufferedImage,
                     color: Color,
                     width: Int,
                     height: Int): BufferedImage {
        return addRectangle(image, color, width - 50, 0, 35, 35)
    }

    private fun addRectangle(image: BufferedImage,
                             color: Color,
                             x: Int,
                             y: Int,
                             widthRectangle: Int,
                             heightRectangle: Int): BufferedImage {
        var g2d = image.createGraphics()
        g2d.drawRect(x, y, widthRectangle, heightRectangle)
        g2d.color = color
        g2d.fillRect(x, y, widthRectangle, heightRectangle)
        g2d.dispose()
        return image
    }
}