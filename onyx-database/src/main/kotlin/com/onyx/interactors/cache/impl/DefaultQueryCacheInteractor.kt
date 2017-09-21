package com.onyx.interactors.cache.impl

import com.onyx.descriptor.EntityDescriptor
import com.onyx.persistence.IManagedEntity
import com.onyx.persistence.context.SchemaContext
import com.onyx.interactors.cache.data.CachedQueryMap
import com.onyx.persistence.query.Query
import com.onyx.interactors.cache.QueryCacheInteractor
import com.onyx.interactors.cache.data.CachedResults
import com.onyx.persistence.query.QueryListener
import com.onyx.persistence.query.QueryListenerEvent
import com.onyx.depricated.CompareUtil
import com.onyx.extension.partitionId
import com.onyx.fetch.PartitionReference

/**
 * Created by tosborn1 on 3/27/17.
 *
 * This controller handles how a query is cached.  In this case, the policy is if the entity is part of one of the 100
 * most recently used queries.
 *
 * @since 1.3.0 Introduced
 */
open class DefaultQueryCacheInteractor(private val context: SchemaContext) : QueryCacheInteractor {

    // @since 1.3.0 - Changed to CachedQueryMap so we can retain strong references for query subscriptions
    private val cachedQueriesByClass = HashMap<Class<*>, CachedQueryMap<Query, CachedResults>>()

    /**
     * Get Cached results for a query. This method will return a cached query result if it exist.
     *
     * @param query Query to check for results.
     *
     * @return The cached results or null
     * @since 1.3.0
     */
    override fun getCachedQueryResults(query: Query): CachedResults? = synchronized(cachedQueriesByClass) {
        cachedQueriesByClass.getOrPut(query.entityType!!) { CachedQueryMap(100, 5 * 60) }
        }[query]

    /**
     * Set cached query results.  This is typically done on executeQuery and executeLazyQuery.
     *
     * @param query Corresponding query for results
     *
     * @param results Result as references
     */
    override fun setCachedQueryResults(query: Query, results: Map<Any, Any?>): CachedResults {
        val queryCachedResultsMap = synchronized(cachedQueriesByClass) { cachedQueriesByClass.getOrPut(query.entityType!!) { CachedQueryMap(100, 5 * 60) } }

        return synchronized(queryCachedResultsMap) {
            val cachedResults = CachedResults(results.toMutableMap())

            // Set a strong reference if this is a query listener.  In that
            // case we do not want it to get cleaned up.
            if (query.changeListener != null)
                queryCachedResultsMap.putStrongReference(query, cachedResults)
            else
                queryCachedResultsMap.put(query, cachedResults)

            return@synchronized cachedResults
        }

    }

    /**
     * Update all of the cached results if an entity has been modified.  It will re-check the criteria and
     * update the applied cached query results
     *
     * @param entity Entity that was potentially inserted, updated, or deleted.
     * @param descriptor The entity's descriptor
     * @param entityReference The entity's reference
     * @param type Wheter or not to remove it from the cache.  In this case, it would be if an entity was deleted.
     *
     * @since 1.3.0
     */
    override fun updateCachedQueryResultsForEntity(entity: IManagedEntity, descriptor: EntityDescriptor, reference: PartitionReference, type: QueryListenerEvent) {
        val queryCacheMap = synchronized(cachedQueriesByClass) { cachedQueriesByClass[entity::class.java] } ?: return
//        var reference:Any = entityReference

//        if(descriptor.hasPartition)
//            reference = PartitionReference(entity.partitionId(context, descriptor), entityReference)

        queryCacheMap.forEach { query, cachedResults ->
            // If indicated to remove the record, delete it and move on
            if (type != QueryListenerEvent.INSERT && type != QueryListenerEvent.UPDATE) {
                synchronized(cachedResults!!) {
                    cachedResults.remove(reference, entity, type, CompareUtil.meetsCriteria(query.getAllCriteria(), query.criteria!!, entity, reference, context, descriptor))
                }
            } else if (CompareUtil.meetsCriteria(query.getAllCriteria(), query.criteria!!, entity, reference, context, descriptor)) {

                synchronized(cachedResults!!) {
                    if (query.selections != null && query.selections!!.isNotEmpty()) {
                        cachedResults.put(reference, reference, type)
                    } else {
                        cachedResults.put(reference, entity, type)
                    }
                }
            }
        }
    }

    /**
     * This method is used to subscribe irrespective of a query being ran.
     * @param query Query object with defined listener
     *
     * @since 1.3.1
     */
    override fun subscribe(query: Query) {
        val queryCachedResultsMap = synchronized(cachedQueriesByClass) { cachedQueriesByClass.getOrPut(query.entityType!!) { CachedQueryMap(100, 5 * 60) } }
        queryCachedResultsMap.compute(query) { _, results ->
            return@compute if (results != null) {
                results.subscribe(query.changeListener!!)
                results
            } else {
                val newResults = CachedResults(null)
                newResults.subscribe(query.changeListener!!)
                newResults
            }
        }
    }

    /**
     * Subscribe a query listener with associated cached results.
     * The subscription will ocur during executeQuery and executeLazyQuery.
     * Currently you cannot subscribe to delete or update queries.
     *
     * @param results Results to listen to
     * @param queryListener listener to respond to cache changes
     *
     * @since 1.3.0
     */
    override fun subscribe(results: CachedResults, queryListener: QueryListener<*>) = results.subscribe(queryListener)

    /**
     * Un-subscribe query.  This must be done manually.  The un-subscribe will not
     * be done auto-magically.  This is highlighted as a danger of using this
     * functionality.
     *
     * @param query Query to un-subscribe from
     * @return Whether the listener was listening to begin with
     *
     * @since 1.3.0
     */
    override fun unSubscribe(query: Query): Boolean {
        val cachedResults = getCachedQueryResults(query)
        return cachedResults != null && cachedResults.unSubscribe(query.changeListener!!)
    }

    /**
     * Cache query results from the closure.  If the query has already been cached, return the results
     * of the cache.
     *
     * @param query Query results to cache
     * @param body Closure to execute to retrieve the results of the query
     *
     * @since 2.0.0
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Map<Any, Any?>> cache(query: Query, body: () -> T): T {
        var cachedResults:CachedResults? = null
        try {

            // Check for cached query results.
            cachedResults = getCachedQueryResults(query)
            val results: T

            // The query has already been cached.  Return the results from the cache
            if (cachedResults?.references != null) {
                results = cachedResults.references as T
            } else {
                // There were no cached results, load them from the store
                results = body.invoke()
                if(cachedResults == null)
                    cachedResults = setCachedQueryResults(query, results as Map<Any, Any?>)
                else
                    synchronized(cachedResults) {
                        cachedResults!!.references = results as MutableMap<Any, Any?>
                    }
            }

            query.resultsCount = results.size
            return results
        } finally {
            if (query.changeListener != null) {
                subscribe(cachedResults!!, query.changeListener!!)
            }
        }
    }
}

