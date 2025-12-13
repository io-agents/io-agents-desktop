package com.pawlowski.io_agents_desktop.domain.plantUml

import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import java.io.ByteArrayOutputStream
import java.io.File

internal fun generateUmlImage(
    umlSource: String,
    outputPath: String,
) {
    val reader = SourceStringReader(umlSource)
    val out = ByteArrayOutputStream()

    val description = reader.outputImage(out)
    check(reader.blocks.isNotEmpty()) { "No UML blocks found in the source." }
    check(description.description.contains("Error").not()) {
        val out2 = ByteArrayOutputStream()
        reader.outputImage(out2, FileFormatOption(FileFormat.UTXT))
        "Error generating UML diagram: ${description.description} $out2"
    }
    File(outputPath).writeBytes(out.toByteArray())
}

