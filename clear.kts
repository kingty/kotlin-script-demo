import java.io.File

val pattern = """@mipmap\/[a-zA-Z0-9_]+[^a-zA-Z0-9_]|R\.mipmap\.[A-Za-z0-9_]+[^a-zA-Z0-9_]""".toPattern()
val resDirs = sequenceOf("-hdpi", "-mdpi", "-xhdpi", "-xxhdpi", "-xxxhdpi").map {"app/src/main/res/mipmap$it" }.asSequence()
val drawables = sequenceOf("png", "jpg", "xml")
val codes = sequenceOf("kt", "java", "xml")
val projects = sequenceOf("app")

fun getFileName(f: File): String {
    return f.name.replace(".9.png", "").replace(".png", "").replace(".jpg", "").replace(".xml", "")
}

fun fileToString(fn: File) = fn.readLines().filter { !it.trim().startsWith("//") }.asSequence().joinToString("\n")

fun allFiles(f: File, extensions: Sequence<String>): Sequence<File> {
    val files = ArrayList<File>()
    if (f.isDirectory) {
        f.listFiles().map {files.addAll(allFiles(it, extensions)) }
    } else if (extensions.any { f.name.endsWith("." + it) }) {
        files.add(f)
    }
    return files.asSequence()
}

fun reses(): Sequence<File> = resDirs.map { allFiles(File(it), drawables) }.flatten()

fun allNames() = reses().map { getFileName(it) }.toSet()

val usedNames = projects.map { p ->
    allFiles(File(p + "/src"), codes).map {fileToString(it) }.map {
        val names = ArrayList<String>()
        val m = pattern.matcher(it)
        while (m.find()) {
            names.add(m.toMatchResult().group().dropLast(1).replace("@mipmap/", "").replace("R.mipmap.", ""))
        }
        names.asSequence()
    }.flatten()
}.flatten().toSet()

val unusedNames = allNames().filter { !usedNames.contains(it) }.toSet()

fun cleanDir(f: File) {
    if (f.isDirectory) {
        f.listFiles().forEach { cleanDir(it) }
    } else {
        if (unusedNames.map { getFileName(f) == it }.toSet().contains(true)) {
            f.delete()
        }
    }
}

fun clean() = resDirs.map { File(it) }.forEach { cleanDir(it) }

clean()
