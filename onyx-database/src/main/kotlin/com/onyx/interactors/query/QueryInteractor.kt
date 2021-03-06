package com.onyx.interactors.query

import com.onyx.exception.OnyxException
import com.onyx.interactors.record.data.Reference
import com.onyx.persistence.query.Query

interface QueryInteractor {

    /**
     * Find object ids that match the criteria
     *
     * @param query Query Criteria
     * @return References matching query criteria
     * @throws com.onyx.exception.OnyxException General query exception
     * @since 1.3.0 This has been refactored to remove the logic for meeting criteria.  That has
     * been moved to CompareUtil
     */
    @Throws(OnyxException::class)
    fun <T : Any?> getReferencesForQuery(query: Query): MutableMap<Reference, T>

    /**
     * Sort using order by query order objects with included values
     *
     * @param query           Query containing order instructions
     * @param referenceValues Query reference values from result of scan
     * @return Sorted references
     * @throws OnyxException Error sorting objects
     */
    fun <T : Any?> sort(query: Query, referenceValues: MutableMap<Reference, T>): MutableMap<Reference, T>

    /**
     * Sort a set of hydrated results.  This is in the event of aggregation or grouping on data that
     * sorting the references is not sufficient
     *
     * @since 2.1.0
     */
    fun <T : Any?> sort(query: Query, referenceValues: List<T>): List<T>

    /**
     * Hydrate a subset of records with the given identifiers
     *
     * @param query      Query containing all the munging instructions
     * @param references References from query results
     * @return Hydrated entities
     * @throws OnyxException Error hydrating entities
     */
    @Throws(OnyxException::class)
    fun <T : Any?> referencesToResults(query: Query, references: MutableMap<Reference, T>): List<T>

    /**
     * Get grouped by results.  This is in the event a query has specified groupBy and has selections.
     *
     * This will aggregate and bunch results by a unique attribute value.
     *
     * @since 2.1.0 Added Group by and query functions
     */
    fun <T> getGroupByResults(query:Query, results:List<T>):HashMap<String, MutableMap<Any?,ArrayList<Any?>>>

    /**
     * Aggregate grouped results and calculate query functions.
     *
     * @since 2.1.0 Added Group by and query functions
     */
    fun <T> getQueryFunctionResults(query:Query, groupedResults:HashMap<String, MutableMap<Any?, ArrayList<Any?>>>):List<T>

    /**
     * Pivot results to only get function results
     *
     * @since 2.1.0 Added query functions such as count(), avg(), min(), max()
     */
    fun <T> getFlatQueryFunctionResults(query:Query, groupedResults:List<T>):List<T>

    /**
     * Return a subset of records based on query maxResults & firstRow
     *
     * @param query      Query containing all the munging instructions
     * @param references References from query results
     * @return Range of records if specified to do so in query
     * @throws OnyxException Error hydrating entities
     */
    fun <T : Any?> filterReferences(query: Query, references: MutableMap<Reference, T>): MutableMap<Reference, T>

    /**
     * Hydrate given attributes
     *
     * @param query      Query containing selection and count information
     * @param references References found during query execution
     * @return Hydrated key value set for entity attributes
     * @throws OnyxException Cannot hydrate entities
     */
    @Throws(OnyxException::class)
    fun <T : Any?> referencesToSelectionResults(query: Query, references: Map<Reference, T>): List<T>

    /**
     * Delete record with reference ids
     *
     * @param records References to delete
     * @param query   Query object
     * @return Number of entities deleted
     * @throws OnyxException Cannot delete entities
     */
    @Throws(OnyxException::class)
    fun <T : Any?> deleteRecordsWithReferences(records: Map<Reference, T>, query: Query): Int

    /**
     * Update records
     *
     * @param query   Query information containing update values
     * @param records Entity references as a result of the query
     * @return how many entities were updated
     * @throws OnyxException Cannot update entity
     */
    @Throws(OnyxException::class)
    fun <T : Any?> updateRecordsWithReferences(query: Query, records: Map<Reference, T>): Int

    /**
     * Get the count for a query.  This is used to get the count without actually executing the query.  It is lighter weight
     * than the entire query and in most cases will use the longSize on the disk map data structure if it is
     * for the entire table.
     *
     * @param query Query to identify count for
     * @return The number of records matching query criterion
     * @throws OnyxException Exception occurred while executing query
     * @since 1.3.0 Added as enhancement #71
     */
    @Throws(OnyxException::class)
    fun getCountForQuery(query: Query): Long

    /**
     * Cleanup the query controller references so that we do not have memory leaks.
     * The most important part of this is to recycle the temporary map builders.
     */
    fun cleanup()
}