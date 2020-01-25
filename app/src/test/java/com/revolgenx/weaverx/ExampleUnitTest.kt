package com.revolgenx.weaverx

import org.junit.Test

import org.junit.Assert.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun testErrorCode() {
        val pattern = Pattern.compile("errorCode：([0-9]*)")
        val str =
            "        //2020-01-24 20:28:38.093 1872-1872/com.example.weaverx E/AddBookBottomSheetDialog: com.arialyy.aria.exception.AriaIOException: Aria Net Exception:任务下载失败，errorCode：404, url: http://ipv4.download.thinkbroadband.com/70MB.zip\n"
        val matcher = pattern.matcher(str)
        if (matcher.find()) {
            val code = matcher.group(1)
            println(code)
            assertTrue(code == "404")
        }else{
            assertTrue(false)
        }
    }
}
