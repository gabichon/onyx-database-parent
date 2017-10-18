package com.onyx.client.rmi

import com.onyx.client.CommunicationPeer

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*

/**
 * Created by tosborn1 on 6/27/16.
 *
 *
 * The purpose of this is to serve as a thin client for retrieving proxy remote instances from the server
 *
 * @since 1.2.0
 */
class OnyxRMIClient : CommunicationPeer() {


    // Local Cache of Remote Objects
    private val registeredObjects = HashMap<String, Any>()

    /**
     * Get a Remote Proxy Object
     *
     * @param remoteId Instance name of the registered object
     * @param type     The class type of what you are trying to get
     * @return Instance of the remote proxy object
     * @since 1.2.0
     */
    fun getRemoteObject(remoteId: String, type: Class<*>): Any? {

        // Return the registered Object
        if (registeredObjects.containsKey(remoteId))
            return registeredObjects[remoteId]

        // Create an array to feed to the Proxy factory
        val interfaces = arrayOfNulls<Class<*>>(1)
        interfaces[0] = type

        val instance = Proxy.newProxyInstance(type.classLoader, interfaces, RMIClientInvocationHander(type, remoteId))

        // Add it to the local cache
        registeredObjects.put(remoteId, instance)
        return instance
    }


    /**
     * This class is added in order to support tracking of methods.
     * Rather than sending in the string value of a method, this is optimized
     * to use the sorted index of a method so the packet is reduced from
     * a string to a single byte.
     *
     * @since 1.3.0
     */
    private inner class RMIClientInvocationHander internal constructor(type: Class<*>, internal val remoteId: String) : InvocationHandler {
        internal var methods: List<Method> = ArrayList()

        init {
            val methodArray = type.declaredMethods
            this.methods = Arrays.asList(*methodArray)
            Collections.sort(this.methods) { o1, o2 -> o1.toString().compareTo(o2.toString()) }
        }

        @Throws(Throwable::class)
        override fun invoke(proxy: Any, method: Method, args: Array<Any>): Any? {
            val request = RMIRequest(remoteId, methods.indexOf(method).toByte(), args)
            val result = send(request)
            if (result is Exception)
                throw result
            return result
        }
    }
}