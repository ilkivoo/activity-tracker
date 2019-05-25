package activitytracker

import activitytracker.log.Logger
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys


class CodeWritingProcess : AnAction("СodeWritingProcess") {
    override fun actionPerformed(e: AnActionEvent?) {
        if (e == null) {
            return
        }
        Logger.save()
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val curCode = editor.document.charsSequence.toString()
        val diffs = Logger.getLogs()
        VideoCreator(curCode).create(diffs)
        //todo окно что видео сохранено
    }
}
