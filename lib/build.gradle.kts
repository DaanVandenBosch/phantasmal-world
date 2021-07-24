import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import java.io.PrintWriter

plugins {
    id("world.phantasmal.multiplatform")
    kotlin("plugin.serialization")
}

buildscript {
    dependencies {
        classpath("org.snakeyaml:snakeyaml-engine:2.1")
    }
}

val serializationVersion: String by project.extra

kotlin {
    sourceSets {
        commonMain {
            kotlin.setSrcDirs(kotlin.srcDirs + file("build/generated-src/commonMain/kotlin"))
            dependencies {
                api(project(":core"))
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
            }
        }

        commonTest {
            dependencies {
                implementation(project(":test-utils"))
            }
        }
    }
}

val generateOpcodes = tasks.register("generateOpcodes") {
    group = "code generation"

    val packageName = "world.phantasmal.lib.asm"
    val opcodesFile = file("srcGeneration/asm/opcodes.yml")
    val outputFile = file(
        "build/generated-src/commonMain/kotlin/${packageName.replace('.', '/')}/Opcodes.kt"
    )

    inputs.file(opcodesFile)
    outputs.file(outputFile)

    @Suppress("UNCHECKED_CAST")
    doLast {
        val root = Load(LoadSettings.builder().build())
            .loadFromInputStream(opcodesFile.inputStream()) as Map<String, Any>

        outputFile.printWriter()
            .use { writer ->
                writer.println("@file:Suppress(\"unused\")")
                writer.println()
                writer.println("package $packageName")
                writer.println()
                writer.println("val OPCODES: Array<Opcode?> = Array(256) { null }")
                writer.println("val OPCODES_F8: Array<Opcode?> = Array(256) { null }")
                writer.println("val OPCODES_F9: Array<Opcode?> = Array(256) { null }")

                (root["opcodes"] as List<Map<String, Any>>).forEach { opcode ->
                    opcodeToCode(writer, opcode)
                }
            }
    }
}

fun opcodeToCode(writer: PrintWriter, opcode: Map<String, Any>) {
    val code = (opcode["code"] as String).drop(2).toInt(16)
    val codeStr = code.toString(16).toUpperCase().padStart(2, '0')
    val mnemonic = opcode["mnemonic"] as String? ?: "unknown_${codeStr.toLowerCase()}"
    val doc = (opcode["doc"] as String?)?.let {
        "\"${it.replace("\n", "\\n")}\""
    }
    val stack = opcode["stack"] as String?

    val valName = "OP_" + mnemonic
        .replace("!=", "ne")
        .replace("=", "e")
        .replace("<", "l")
        .replace(">", "g")
        .toUpperCase()

    val stackInteraction = when (stack) {
        "push" -> "StackInteraction.Push"
        "pop" -> "StackInteraction.Pop"
        else -> "null"
    }

    @Suppress("UNCHECKED_CAST")
    val params = opcode["params"] as List<Map<String, Any>>
    val paramsStr = paramsToCode(params, 4)

    val lastParam = params.lastOrNull()
    val varargs = lastParam != null && when (lastParam["type"]) {
        null -> error("No type for last parameter of $mnemonic opcode.")
        "ilabel_var", "reg_var" -> true
        else -> false
    }

    val known = "mnemonic" in opcode

    val array = when (code) {
        in 0x00..0xFF -> "OPCODES"
        in 0xF800..0xF8FF -> "OPCODES_F8"
        in 0xF900..0xF9FF -> "OPCODES_F9"
        else -> error("Invalid opcode $codeStr ($mnemonic).")
    }
    val indexStr = (code and 0xFF).toString(16).toUpperCase().padStart(2, '0')

    writer.println(
        """
        |
        |val $valName = Opcode(
        |    code = 0x$codeStr,
        |    mnemonic = "$mnemonic",
        |    doc = $doc,
        |    params = $paramsStr,
        |    stack = $stackInteraction,
        |    varargs = $varargs,
        |    known = $known,
        |).also { ${array}[0x$indexStr] = it }""".trimMargin()
    )
}

fun paramsToCode(params: List<Map<String, Any>>, indent: Int): String {
    val i = " ".repeat(indent)

    if (params.isEmpty()) return "emptyList()"

    return params.joinToString(",\n", "listOf(\n", ",\n${i})") { param ->
        @Suppress("UNCHECKED_CAST")
        val type = when (param["type"]) {
            "any" -> "AnyType.Instance"
            "byte" -> "ByteType"
            "short" -> "ShortType"
            "int" -> "IntType"
            "float" -> "FloatType"
            "label" -> "LabelType.Instance"
            "ilabel" -> "ILabelType"
            "dlabel" -> "DLabelType"
            "slabel" -> "SLabelType"
            "string" -> "StringType"
            "ilabel_var" -> "ILabelVarType"
            "reg" -> """RegType(${
                (param["registers"] as List<Map<String, Any>>?)?.let {
                    paramsToCode(it, indent + 4)
                } ?: "null"
            })"""
            "reg_var" -> "RegVarType"
            "pointer" -> "PointerType"
            else -> error("Type ${param["type"]} not implemented.")
        }

        val name = (param["name"] as String?)?.let { "\"$it\"" } ?: "null"
        val doc = (param["doc"] as String?)?.let { "\"$it\"" } ?: "null"
        val read = param["read"] as Boolean? == true
        val write = param["write"] as Boolean? == true

        "$i    Param(${type}, ${name}, ${doc}, ${read}, ${write})"
    }
}

// The following line results in warning "The AbstractCompile.destinationDir property has been
// deprecated.".
tasks.withType<AbstractKotlinCompile<*>>().configureEach {
    dependsOn(generateOpcodes)
}
