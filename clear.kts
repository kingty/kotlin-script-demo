import java.io.File

val pattern = """@mipmap\/[a-zA-Z0-9_]+[^a-zA-Z0-9_]|R\.mipmap\.[A-Za-z0-9_]+[^a-zA-Z0-9_]""".toPattern()
val resDirs = sequenceOf("-hdpi", "-mdpi", "-xhdpi", "-xxhdpi", "-xxxhdpi")
val drawables = sequenceOf("png", "jpg", "xml")
val codes = sequenceOf("kt", "java", "xml")
val projects = sequenceOf("app")

fun getname(f: File): String {
    return f.name.replace(".9.png", "").replace(".png", "").replace(".jpg", "").replace(".xml", "")
}

fun fileString(fn: File) = fn.readLines().filter { l -> !l.trim().startsWith("//") }.asSequence().joinToString("\n")

fun allFiles(f: File, extensions: Sequence<String>): Sequence<File> {
    val files = ArrayList<File>()
    if (f.isDirectory) {
        f.listFiles().map { f -> files.addAll(allFiles(f, extensions)) }
    } else if (extensions.any { ex -> f.name.endsWith("." + ex) }) {
        files.add(f)
    }
    return files.asSequence()
}

fun reses(): Sequence<File> {
    return resDirs.map { d -> "app/src/main/res/mipmap$d" }.map { str -> allFiles(File(str), drawables) }.flatten()
}

fun all_names() = reses().map { file -> getname(file) }.toSet()

fun used_names(): Set<String> {
    return projects.map { p ->
        allFiles(File(p + "/src"), codes).map { f -> fileString(f) }.map { str ->
            val names = ArrayList<String>()
            val m = pattern.matcher(str)
            while (m.find()) {
                names.add(m.toMatchResult().group().dropLast(1).replace("@mipmap/", "").replace("R.mipmap.", ""))
            }
            names.asSequence()
        }.flatten()
    }.flatten().toSet()
}

val unused_names = reses().map { file -> getname(file) }.filter { d -> !used_names().contains(d) }.toSet()


fun cleanDir(f: File) {
    if (f.isDirectory) {
        f.listFiles().forEach { f -> cleanDir(f) }
    } else {
        if (unused_names.map { s -> getname(f) == s }.toSet().contains(true)) {
            f.delete()
        }
    }
}

fun clean() = resDirs.map { d -> "app/src/main/res/mipmap$d" }.map { s -> File(s) }.forEach { f -> cleanDir(f) }

clean()
