package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.Lesson
import java.io.File
import java.io.FileOutputStream

object StudyResourceLauncher {

    fun openExternalUrl(context: Context, rawUrl: String): Result<Unit> {
        return try {
            val cleanUrl = rawUrl.trim()
            if (cleanUrl.isEmpty()) {
                return Result.failure(IllegalArgumentException("الرابط غير متاح حاليًا"))
            }
            val uri = Uri.parse(
                if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                    cleanUrl
                } else {
                    "https://$cleanUrl"
                }
            )
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Opens an external PDF URL if provided; otherwise generates a clean, real A4 PDF study guide
     * directly on-device from the lesson's Arabic content and opens/shares it via FileProvider.
     */
    fun openOrGenerateLessonPdf(
        context: Context,
        lesson: Lesson,
        subjectName: String
    ): Result<String> {
        val externalUrl = lesson.pdfUrl?.trim().orEmpty()
        if (externalUrl.startsWith("http://") || externalUrl.startsWith("https://")) {
            return openExternalUrl(context, externalUrl).map { "تم فتح ملف PDF الخارجي" }
        }

        return try {
            val pdfDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val safeId = lesson.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "lesson" }
            val pdfFile = File(pdfDir, "mesaha_${safeId}.pdf")

            generateArabicLessonPdf(pdfFile, subjectName, lesson)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(viewIntent)
            } catch (_: Exception) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, lesson.pdfTitle ?: lesson.title)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "فتح أو مشاركة ملخص الدرس PDF").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
            Result.success("تم إنشاء وتجهيز ملف PDF بنجاح")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateArabicLessonPdf(outputFile: File, subjectName: String, lesson: Lesson) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 44f
        val contentWidth = (pageWidth - margin * 2).toInt()

        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#0B402E")
            style = Paint.Style.FILL
        }
        val accentLinePaint = Paint().apply {
            color = Color.parseColor("#C8A252")
            strokeWidth = 3f
        }
        val headerTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D8F0E5")
            textSize = 13f
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0E241C")
            textSize = 13.5f
        }
        val summaryPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#135D43")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val fullBodyText = buildString {
            appendLine("الملخص التنفيذي للدرس:")
            appendLine(lesson.summary)
            appendLine()
            appendLine("الشرح العلمي التفصيلي:")
            appendLine(lesson.contentText)
            if (!lesson.videoUrl.isNullOrBlank()) {
                appendLine()
                appendLine("رابط الفيديو التعليمي المرجعي:")
                appendLine(lesson.videoUrl)
            }
        }

        val fullLayout = StaticLayout.Builder.obtain(
            fullBodyText, 0, fullBodyText.length, bodyPaint, contentWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(6f, 1.15f)
            .build()

        var currentLine = 0
        var pageNumber = 1

        while (currentLine < fullLayout.lineCount || pageNumber == 1) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            var yCursor = margin

            if (pageNumber == 1) {
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 118f, headerBgPaint)
                canvas.drawLine(0f, 118f, pageWidth.toFloat(), 118f, accentLinePaint)

                val academyLabel = "أكاديمية المساحة - الفرقة الأولى بمعهد المساحة"
                val titleLayout = StaticLayout.Builder.obtain(
                    academyLabel, 0, academyLabel.length, headerSubPaint, contentWidth
                ).setAlignment(Layout.Alignment.ALIGN_OPPOSITE).build()
                canvas.save()
                canvas.translate(margin, 24f)
                titleLayout.draw(canvas)
                canvas.restore()

                val lessonHeading = "${subjectName} | ${lesson.title}"
                val lessonLayout = StaticLayout.Builder.obtain(
                    lessonHeading, 0, lessonHeading.length, headerTitlePaint, contentWidth
                ).setAlignment(Layout.Alignment.ALIGN_OPPOSITE).build()
                canvas.save()
                canvas.translate(margin, 52f)
                lessonLayout.draw(canvas)
                canvas.restore()

                yCursor = 142f
                val badge = "ملف دراسي مجاني مُعد للطباعة والمراجعة"
                val badgeLayout = StaticLayout.Builder.obtain(
                    badge, 0, badge.length, summaryPaint, contentWidth
                ).setAlignment(Layout.Alignment.ALIGN_OPPOSITE).build()
                canvas.save()
                canvas.translate(margin, yCursor)
                badgeLayout.draw(canvas)
                canvas.restore()
                yCursor += badgeLayout.height + 16f
            }

            val availableHeight = pageHeight - yCursor - 48f
            val startLine = currentLine
            var endLine = startLine
            while (endLine < fullLayout.lineCount) {
                val lineHeightSum = fullLayout.getLineBottom(endLine) - fullLayout.getLineTop(startLine)
                if (lineHeightSum > availableHeight && endLine > startLine) break
                endLine++
            }

            if (startLine < endLine) {
                val chunkStart = fullLayout.getLineStart(startLine)
                val chunkEnd = fullLayout.getLineEnd(endLine - 1)
                val chunkText = fullBodyText.substring(chunkStart, chunkEnd)
                val chunkLayout = StaticLayout.Builder.obtain(
                    chunkText, 0, chunkText.length, bodyPaint, contentWidth
                )
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(6f, 1.15f)
                    .build()
                canvas.save()
                canvas.translate(margin, yCursor)
                chunkLayout.draw(canvas)
                canvas.restore()
            }

            val footerText = "أكاديمية المساحة — صفحة $pageNumber"
            canvas.drawText(footerText, margin, pageHeight - 22f, headerSubPaint.apply {
                color = Color.parseColor("#4B6359")
            })

            document.finishPage(page)
            currentLine = endLine
            pageNumber++
            if (currentLine >= fullLayout.lineCount) break
        }

        FileOutputStream(outputFile).use { stream ->
            document.writeTo(stream)
        }
        document.close()
    }
}
