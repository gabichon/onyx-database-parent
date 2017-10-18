package com.onyx.client

import com.onyx.client.exception.ConnectionFailedException
import com.onyx.client.exception.OnyxServerException

/**
 * Created by tosborn1 on 6/26/16.
 *
 * This interface is the contract for how the client connects to the server
 */
interface OnyxClient {

    /**
     * Connect to host with port number
     * @param host Server Host
     * @param port Server Port
     */
    @Throws(ConnectionFailedException::class)
    fun connect(host: String, port: Int)

    /**
     * Write request object to server
     *
     * @param packet Object to send to server
     * @param consumer Consumer for the results
     */
    @Throws(OnyxServerException::class)
    fun send(packet: Any, consumer: (Any?) -> Unit):Any?

    /**
     * A blocking api for sending a request and waiting on the results
     *
     * @param packet Object to send to server
     * @return Object returned from the server
     */
    @Throws(OnyxServerException::class)
    fun send(packet: Any): Any?

    /**
     * Close the connection
     */
    fun close()

    /**
     * Indicator to see if the client is connected to the server
     * @return true or false
     */
    val isConnected: Boolean

    /**
     * Get the timeout in seconds for a request
     * @since 1.2.0
     * @return timeout
     */
    var timeout: Int

}