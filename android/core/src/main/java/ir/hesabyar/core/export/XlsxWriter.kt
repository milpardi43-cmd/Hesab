package ir.hesabyar.core.export

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Minimal, dependency-free XLSX writer (Office Open XML, inline strings).
 * Produces a real .xlsx file that Excel, LibreOffice and Google Sheets can open.
 */
object XlsxWriter {

    sealed interface Cell {
        data class Text(val value: String) : Cell
        data class Number(val value: Long) : Cell
        data class Decimal(val value: Double) : Cell
        data object Empty : Cell
    }

    data class Sheet(val name: String, val headers: List<String>, val rows: List<List<Cell>>)

    fun write(sheets: List<Sheet>): ByteArray {
        require(sheets.isNotEmpty()) { "at least one sheet" }
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.put("[Content_Types].xml", contentTypes(sheets.size))
            zip.put("_rels/.rels", rootRels())
            zip.put("xl/workbook.xml", workbook(sheets))
            zip.put("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            zip.put("xl/styles.xml", STYLES)
            sheets.forEachIndexed { index, sheet ->
                zip.put("xl/worksheets/sheet${index + 1}.xml", worksheet(sheet))
            }
        }
        return out.toByteArray()
    }

    private fun ZipOutputStream.put(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun contentTypes(count: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        repeat(count) { index ->
            append("""<Override PartName="/xl/worksheets/sheet${index + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        append("</Types>")
    }

    private fun rootRels(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
           <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
           <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
           </Relationships>"""

    private fun workbook(sheets: List<Sheet>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""")
        sheets.forEachIndexed { index, sheet ->
            append("""<sheet name="${escape(sheet.name.take(31))}" sheetId="${index + 1}" r:id="rId${index + 1}"/>""")
        }
        append("</sheets></workbook>")
    }

    private fun workbookRels(count: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        repeat(count) { index ->
            append("""<Relationship Id="rId${index + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${index + 1}.xml"/>""")
        }
        append("""<Relationship Id="rId${count + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        append("</Relationships>")
    }

    private fun worksheet(sheet: Sheet): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        appendRow(1, sheet.headers.map { Cell.Text(it) }, header = true)
        sheet.rows.forEachIndexed { index, row ->
            appendRow(index + 2, row, header = false)
        }
        append("</sheetData></worksheet>")
    }

    private fun StringBuilder.appendRow(rowNumber: Int, cells: List<Cell>, header: Boolean) {
        append("""<row r="$rowNumber">""")
        cells.forEachIndexed { index, cell ->
            val reference = columnName(index) + rowNumber
            when (cell) {
                is Cell.Text -> append("""<c r="$reference" t="inlineStr"${if (header) " s=\"1\"" else ""}><is><t xml:space="preserve">${escape(cell.value)}</t></is></c>""")
                is Cell.Number -> append("""<c r="$reference"><v>${cell.value}</v></c>""")
                is Cell.Decimal -> append("""<c r="$reference"><v>${cell.value}</v></c>""")
                Cell.Empty -> Unit
            }
        }
        append("</row>")
    }

    private fun columnName(index: Int): String {
        var value = index + 1
        val sb = StringBuilder()
        while (value > 0) {
            val remainder = (value - 1) % 26
            sb.append(('A' + remainder))
            value = (value - 1) / 26
        }
        return sb.reverse().toString()
    }

    private fun escape(text: String): String = buildString {
        text.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }

    private val STYLES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
           <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
           <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
           <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
           <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
           <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
           <cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs>
           <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
           </styleSheet>"""
}
