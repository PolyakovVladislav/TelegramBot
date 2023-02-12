package utils

import java.util.regex.Pattern

fun String.search(start: String, end: String, include: Boolean, onlyFirst: Boolean): ArrayList<String> {
    return privateSearch(this, start, end, include, onlyFirst)
}

fun String.search(start: String, end: String, include: Boolean): String? {
    val al = privateSearch(this, start, end, include, true)
    return if (al.size < 1) null else al[0]
}

fun String.search(start: String, end: String): String? {
    val al = privateSearch(this, start, end, false, true)
    return if (al.size < 1) null else al[0]
}

private fun privateSearch(
    target: String,
    begin: String,
    end: String,
    include: Boolean,
    onlyFirst: Boolean
): ArrayList<String> {
    var internalTarget = target
    val list = ArrayList<String>()
    val pattern = Pattern.compile("$begin.*?$end")
    internalTarget = internalTarget
        .replace("\\t".toRegex(), "\\\\t")
        .replace("\\n".toRegex(), "\\\\n")
        .replace("\\r".toRegex(), "\\\\r")
    val matcher = pattern.matcher(internalTarget)
    var result: String
    while (matcher.find()) {
        if (include) {
            result = internalTarget.substring(matcher.start(), matcher.end())
        } else {
            result = matcher.group()
            result = result.replaceFirst(begin.toRegex(), "")
            result = result.replaceFirst(end.toRegex(), "")
        }
        list.add(result)
        if (onlyFirst) break
    }
    return list
}

fun String.find(mask: String): Boolean {
    val pattern = Pattern.compile(mask)
    val matcher = pattern.matcher(this)
    return matcher.find()
}

fun String.partLeft(delimiter: String): String {
    val words = privateStringPart(this, delimiter)
    return words[0]
}

fun String.partRight(delimiter: String): String {
    val words = privateStringPart(this, delimiter)
    if (words.size < 2) return ""
    var word = words[1]
    var i = 2
    while (words.size > i) {
        word = word + delimiter + words[i]
        i++
    }
    return word
}

fun String.parts(delimiter: String): Array<String> {
    return privateStringPart(this, delimiter)
}

private fun privateStringPart(target: String, delimiter: String): Array<String> {
    val pattern = Pattern.compile(delimiter)
    return pattern.split(target)
}