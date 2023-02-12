package utils

import java.util.regex.Pattern

object Strings {
    /**
     * ^ - начало строки
     * $ - конец строки
     * . - любой символ
     * \s - пробел
     * \d - любая цифра [0-9]
     * \D - любой не цифровой символ
     * [0-1], [123] - любой из группы симвлолов
     * [^0-1] - любой кроме перечисленных
     * \n - начало новой строки
     * \\ - убирает специальное значение спец символов (^, $ и т.д.)
     * Квантификаторы:
     * - ноль или более
     * + - один или более
     * ? - ноль или один
     * {n} - n раз
     * {n,m} - от n до m раз
     * ".+" - жадный режим
     * ".++" - сверхжадный режим
     * ".+?" - ленивый режим
     */
    fun search(target: String, start: String, end: String, include: Boolean, onlyFirst: Boolean): ArrayList<String> {
        return privateSearch(target, start, end, include, onlyFirst)
    }

    fun search(target: String, start: String, end: String, include: Boolean): String {
        val al = privateSearch(target, start, end, include, true)
        return if (al.size < 1) "" else al[0]
    }

    fun search(target: String, start: String, end: String): String {
        val al = privateSearch(target, start, end, false, true)
        return if (al.size < 1) "" else al[0]
    }

    private fun privateSearch(
        target: String,
        begin: String,
        end: String,
        include: Boolean,
        onlyFirst: Boolean
    ): ArrayList<String> {
        var target = target
        val list = ArrayList<String>()
        val pattern = Pattern.compile("$begin.*?$end")
        target = target
            .replace("\\t".toRegex(), "\\\\t")
            .replace("\\n".toRegex(), "\\\\n")
            .replace("\\r".toRegex(), "\\\\r")
        val matcher = pattern.matcher(target)
        var result: String
        while (matcher.find()) {
            if (include) {
                result = target.substring(matcher.start(), matcher.end())
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

    fun find(target: String?, mask: String?): Boolean {
        val pattern = Pattern.compile(mask)
        val matcher = pattern.matcher(target)
        return matcher.find()
    }

    fun stringPartLeft(target: String, delimiter: String): String {
        val words = privateStringPart(target, delimiter)
        return words[0]
    }

    fun stringPartRight(target: String, delimiter: String): String {
        val words = privateStringPart(target, delimiter)
        if (words.size < 2) return ""
        var word = words[1]
        var i = 2
        while (words.size > i) {
            word = word + delimiter + words[i]
            i++
        }
        return word
    }

    fun stringPart(target: String, delimiter: String): Array<String> {
        return privateStringPart(target, delimiter)
    }

    private fun privateStringPart(target: String, delimiter: String): Array<String> {
        val pattern = Pattern.compile(delimiter)
        return pattern.split(target)
    }
}