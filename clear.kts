import java.io.File

val pattern = """@mipmap\/[a-zA-Z0-9_]+[^a-zA-Z0-9_]|R\.mipmap\.[A-Za-z0-9_]+[^a-zA-Z0-9_]""".toPattern()
val resDirs = sequenceOf("-hdpi", "-mdpi", "-xhdpi", "-xxhdpi", "-xxxhdpi").map { s -> "app/src/main/res/mipmap$s" }.asSequence()
val drawables = sequenceOf("png", "jpg", "xml")
val codes = sequenceOf("kt", "java", "xml")
val projects = sequenceOf("app")

fun getFileName(f: File): String {
    return f.name.replace(".9.png", "").replace(".png", "").replace(".jpg", "").replace(".xml", "")
}

fun fileToString(fn: File) = fn.readLines().filter { l -> !l.trim().startsWith("//") }.asSequence().joinToString("\n")

fun allFiles(f: File, extensions: Sequence<String>): Sequence<File> {
    val files = ArrayList<File>()
    if (f.isDirectory) {
        f.listFiles().map { f -> files.addAll(allFiles(f, extensions)) }
    } else if (extensions.any { ex -> f.name.endsWith("." + ex) }) {
        files.add(f)
    }
    return files.asSequence()
}

fun reses(): Sequence<File> = resDirs.map { str -> allFiles(File(str), drawables) }.flatten()

fun allNames() = reses().map { file -> getFileName(file) }.toSet()

val usedNames = projects.map { p ->
    allFiles(File(p + "/src"), codes).map { f -> fileToString(f) }.map { str ->
        val names = ArrayList<String>()
        val m = pattern.matcher(str)
        while (m.find()) {
            names.add(m.toMatchResult().group().dropLast(1).replace("@mipmap/", "").replace("R.mipmap.", ""))
        }
        names.asSequence()
    }.flatten()
}.flatten().toSet()

val unusedNames = allNames().filter { d -> !usedNames.contains(d) }.toSet()

fun cleanDir(f: File) {
    if (f.isDirectory) {
        f.listFiles().forEach { f -> cleanDir(f) }
    } else {
        if (unusedNames.map { s -> getFileName(f) == s }.toSet().contains(true)) {
            f.delete()
        }
    }
}

fun clean() = resDirs.map { s -> File(s) }.forEach { f -> cleanDir(f) }

clean()
