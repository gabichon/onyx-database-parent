package com.onyx.network.push

import com.onyx.exception.OnyxServerException

/**
 * Created by Tim Osborn on 3/27/17.
 *
 * Register a consumer with the given subscriber information
 */
interface PushRegistrar {

    /**
     * Register a consumer with a subscriber
     * @param consumer Consumes the push notifications
     * @param responder Uniquely identifies a subscriber
     * @throws OnyxServerException Communication error
     *
     * @since 1.3.0
     */
    @Throws(OnyxServerException::class)
    fun register(consumer: PushSubscriber, responder: PushConsumer)

    /**
     * De register a push registration.  This API is for clients to take the original subscriber containing
     * the push identity and send it off to the server to de-register.
     *
     * Note: There is no receipt for this action
     *
     * @param subscriber Subscriber originally registered
     * @throws OnyxServerException Communication Exception
     *
     * @since 1.3.0
     */
    @Throws(OnyxServerException::class)
    fun unregister(subscriber: PushSubscriber)

}
