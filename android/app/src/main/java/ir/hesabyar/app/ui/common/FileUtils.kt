package ir.hesabyar.app.ui.common

import android.content.Context
import android.net.Uri
import java.io.File

object FileUtils {
    fun copyUriToInternal(context: Context, uri: Uri, prefix: String = "attachment"): String? = runCatching {
        val dir = File(context.filesDir, "attachments").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file.absolutePath
    }.getOrNull()
}
