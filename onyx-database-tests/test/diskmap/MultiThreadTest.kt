package diskmap

import com.onyx.diskmap.factory.impl.DefaultDiskMapFactory
import database.base.DatabaseBaseTest
import org.junit.Test

import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.test.assertNull

/**
 * Created by timothy.osborn on 3/27/15.
 */
class MultiThreadTest : AbstractTest() {

    companion object {
        private var threadPool = Executors.newFixedThreadPool(10)
    }

    @Test
    fun testMultiThread() {
        val store = DefaultDiskMapFactory(AbstractTest.TEST_DATABASE)
        val myMap = store.getHashMap<MutableMap<Int, String>>("second")

        val items = ArrayList<Future<*>>()
        for (i in 1..19) {
            val p = DatabaseBaseTest.randomInteger + 1

            if (i % 5 == 0) {
                val scanThread = DatabaseBaseTest.async(threadPool) {
                    val it = myMap.entries.iterator()

                    while (it.hasNext()) {
                        val entry = it.next() as Map.Entry<Int, String>
                        entry.key
                    }
                }
                items.add(scanThread)
            }

            val runnable = MyRunnable(myMap, p)
            items.add(DatabaseBaseTest.async(threadPool, runnable::run))
        }

        items.forEach { it.get() }
        store.close()
    }

    internal inner class MyRunnable(myMap: MutableMap<Int, String>, private var p: Int) : Runnable {
        private var myMap: MutableMap<Int, String>? = null

        init {
            this.myMap = myMap
        }

        override fun run() {
            for (k in 1..4999) {
                val value:Int = (k * p)
                myMap!!.put(value, "HIYA" + value)
                myMap!![value] as String
                myMap!!.remove(value)
                assertNull(myMap!![value])
            }
        }
    }
}
