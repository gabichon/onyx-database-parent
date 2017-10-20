import com.onyx.client.auth.AuthenticationManager
import com.onyx.client.rmi.OnyxRMIClient
import com.onyx.extension.common.async
import com.onyx.server.rmi.OnyxRMIServer
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import org.apache.http.auth.AUTH
import org.junit.Test
import rmi.IMessage
import rmi.MessageImplementation
import rmi.RMIClient
import rmi.RMIMessageServer

import java.rmi.RemoteException
import java.util.ArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Consumer

class RMITest {

    @Test
    @Throws(Exception::class)
    fun testOnyxRMIPerformance() {
        val rmiServer = OnyxRMIServer()
        rmiServer.port = 8080
        rmiServer.register("A", MessageImplementation(), IMessage::class.java)
        rmiServer.register("AUTH", object : AuthenticationManager {
            override fun verify(username: String?, password: String?) {

            }
        }, AuthenticationManager::class.java)
        rmiServer.start()

        val rmiClient = OnyxRMIClient()
        rmiClient.authenticationManager = rmiClient.getRemoteObject("AUTH", AuthenticationManager::class.java) as AuthenticationManager?
        rmiClient.connect("localhost", 8080)
        val messanger = rmiClient.getRemoteObject("A", IMessage::class.java) as IMessage


        val message = ByteArray(2048)
        val countDownLatch = CountDownLatch(10000000)
        val startTime = System.currentTimeMillis()
        val context = newFixedThreadPoolContext(16, "iduno")

        for (i in 0..10000000) {
            async(context) {
                try {
                    messanger.captureMessage(message)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
                countDownLatch.countDown()
            }
        }

        countDownLatch.await()
        val stopTime = System.currentTimeMillis()

        println(stopTime - startTime)

    }

    @Test
    @Throws(Exception::class)
    fun testRMIPerformance() {
        RMIMessageServer.main(null)

        val messenger = RMIClient.getMessage()

        val message = ByteArray(2048)
        val threadPool = Executors.newFixedThreadPool(16)
        val countDownLatch = CountDownLatch(1000000)
        val startTime = System.currentTimeMillis()

        for (i in 0..999999) {
            threadPool.execute {
                try {
                    messenger!!.captureMessage(message)
                    countDownLatch.countDown()
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        }

        countDownLatch.await()
        val stopTime = System.currentTimeMillis()

        println(stopTime - startTime)
    }
}
