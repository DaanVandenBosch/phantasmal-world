import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings
import java.io.PrintWriter

plugins {
    kotlin("multiplatform")
    id("world.phantasmal.gradle.js")
}

buildscript {
    dependencies {
        classpath("org.snakeyaml:snakeyaml-engine:2.1")
    }
}

val coroutinesVersion: String by project.extra
val kotlinLoggingVersion: String by project.extra
val slf4jVersion: String by project.extra

kotlin {
    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    jvm()

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
        }

        commonMain {
            kotlin.setSrcDirs(kotlin.srcDirs + file("build/generated-src/commonMain/kotlin"))
            dependencies {
                api(project(":core"))
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                api("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(project(":test-utils"))
            }
        }

        getByName("jsTest") {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("org.slf4j:slf4j-simple:$slf4jVersion")
            }
        }
    }
}

val generateOpcodes = tasks.register("generateOpcodes") {
    group = "code generation"

    val packageName = "world.phantasmal.lib.assembly"
    val opcodesFile = file("assetsGeneration/assembly/opcodes.yml")
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
    val mnemonic = opcode["mnemonic"] as String? ?: "unknown_$codeStr"
    val description = opcode["description"] as String?
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
    val params = paramsToCode(opcode["params"] as List<Map<String, Any>>, 4)

    val array = when (code) {
        in 0..0xFF -> "OPCODES"
        in 0xF800..0xF8FF -> "OPCODES_F8"
        in 0xF900..0xF9FF -> "OPCODES_F9"
        else -> error("Invalid opcode $codeStr ($mnemonic).")
    }
    val indexStr = (code and 0xFF).toString(16).toUpperCase().padStart(2, '0')

    writer.println(
        """
        |val $valName = Opcode(
        |    0x$codeStr,
        |    "$mnemonic",
        |    ${description?.let { "\"$it\"" }},
        |    $params,
        |    $stackInteraction,
        |).also { ${array}[0x$indexStr] = it }""".trimMargin()
    )
}

fun paramsToCode(params: List<Map<String, Any>>, indent: Int): String {
    val i = " ".repeat(indent)

    if (params.isEmpty()) return "emptyList()"

    return params.joinToString(",\n", "listOf(\n", ",\n${i})") { param ->
        @Suppress("UNCHECKED_CAST")
        val type = when (param["type"]) {
            "any" -> "AnyType()"
            "byte" -> "ByteType"
            "short" -> "ShortType"
            "int" -> "IntType"
            "float" -> "FloatType"
            "label" -> "LabelType()"
            "instruction_label" -> "ILabelType"
            "data_label" -> "DLabelType"
            "string_label" -> "SLabelType"
            "string" -> "StringType"
            "instruction_label_var" -> "ILabelVarType"
            "reg_ref" -> "RegRefType"
            "reg_tup_ref" -> """RegTupRefType(${
                paramsToCode(param["reg_tup"] as List<Map<String, Any>>, indent + 4)
            })"""
            "reg_ref_var" -> "RegRefVarType"
            "pointer" -> "PointerType"
            else -> error("Type ${param["type"]} not implemented.")
        }

        val doc = (param["doc"] as String?)?.let { "\"$it\"" } ?: "null"

        val access = when (param["access"]) {
            "read" -> "ParamAccess.Read"
            "write" -> "ParamAccess.Write"
            "read_write" -> "ParamAccess.ReadWrite"
            else -> "null"
        }

        "$i    Param(${type}, ${doc}, ${access})"
    }
}

tasks.withType<AbstractKotlinCompile<*>> {
    dependsOn(generateOpcodes)
}
