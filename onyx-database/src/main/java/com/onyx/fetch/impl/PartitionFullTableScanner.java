package com.onyx.fetch.impl;

import com.onyx.descriptor.EntityDescriptor;
import com.onyx.diskmap.DiskMap;
import com.onyx.diskmap.MapBuilder;
import com.onyx.diskmap.node.SkipListNode;
import com.onyx.entity.SystemEntity;
import com.onyx.entity.SystemPartitionEntry;
import com.onyx.exception.OnyxException;
import com.onyx.exception.OnyxExceptionWrapper;
import com.onyx.exception.InvalidConstructorException;
import com.onyx.fetch.PartitionReference;
import com.onyx.fetch.TableScanner;
import com.onyx.helpers.PartitionHelper;
import com.onyx.persistence.IManagedEntity;
import com.onyx.persistence.context.SchemaContext;
import com.onyx.persistence.manager.PersistenceManager;
import com.onyx.persistence.query.Query;
import com.onyx.persistence.query.QueryCriteria;
import com.onyx.persistence.query.QueryPartitionMode;
import com.onyx.depricated.CompareUtil;
import com.onyx.util.ReflectionUtil;
import com.onyx.util.map.CompatHashMap;
import com.onyx.util.map.CompatMap;
import com.onyx.util.map.SynchronizedMap;

import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Created by timothy.osborn on 1/3/15.
 * <p>
 * It can either scan the entire table or a subset of index values
 */
public class PartitionFullTableScanner extends FullTableScanner implements TableScanner {

    private SystemEntity systemEntity = null;

    /**
     * Constructor
     *
     * @param criteria    Query Criteria
     * @param classToScan Class type to scan
     * @param descriptor  Entity descriptor of entity type to scan
     */
    public PartitionFullTableScanner(QueryCriteria criteria, Class classToScan, EntityDescriptor descriptor, MapBuilder temporaryDataFile, Query query, SchemaContext context, PersistenceManager persistenceManager) throws OnyxException {
        super(criteria, classToScan, descriptor, temporaryDataFile, query, context, persistenceManager);
        systemEntity = context.getSystemEntityByName(query.getEntityType().getName());
    }


    /**
     * Scan records with existing values
     *
     * @param existingValues Existing values to check for criteria
     * @return Existing values that match the criteria
     * @throws OnyxException Cannot scan partition
     */
    @SuppressWarnings("unchecked")
    private Map scanPartition(DiskMap existingValues, long partitionId) throws OnyxException {
        final CompatMap allResults = new CompatHashMap();

        final Iterator iterator = existingValues.referenceSet().iterator();
        IManagedEntity entity;
        SkipListNode node;

        final SchemaContext context = getContext();

        while (iterator.hasNext()) {
            if (getQuery().isTerminated())
                return allResults;

            node = (SkipListNode)iterator.next();
            entity = (IManagedEntity) existingValues.getWithRecID(node.recordId);

            // Ensure entity still exists
            if (entity == null) {
                continue;
            }

            if(CompareUtil.meetsCriteria(getQuery().getAllCriteria(), getCriteria(), entity, new PartitionReference(partitionId, node.recordId), context, getDescriptor()))
            {
                allResults.put(new PartitionReference(partitionId, node.recordId), new PartitionReference(partitionId, node.recordId));
            }
        }

        return allResults;
    }

    /**
     * Full Table Scan
     *
     * @return References matching criteria
     * @throws OnyxException Cannot scan partition
     */
    @SuppressWarnings("unchecked")
    public Map scan() throws OnyxException {

        final OnyxExceptionWrapper wrapper = new OnyxExceptionWrapper();
        CompatMap<PartitionReference, PartitionReference> results = new SynchronizedMap();

        if (getQuery().getPartition() == QueryPartitionMode.ALL) {

            Iterator<SystemPartitionEntry> it = systemEntity.getPartition().getEntries().iterator();
            CountDownLatch partitionScanCountDown = new CountDownLatch(systemEntity.getPartition().getEntries().size());
            while (it.hasNext()) {
                final SystemPartitionEntry partition = it.next();

                getExecutorService().execute(() -> {
                    try {
                        final EntityDescriptor partitionDescriptor = getContext().getDescriptorForEntity(getQuery().getEntityType(), partition.getValue());

                        final MapBuilder dataFile = getContext().getDataFile(partitionDescriptor);
                        DiskMap recs = (DiskMap) dataFile.getHashMap(partitionDescriptor.getEntityClass().getName(), partitionDescriptor.getIdentifier().getLoadFactor());

                        Map partitionResults = scanPartition(recs, partition.getIndex());
                        results.putAll(partitionResults);
                        partitionScanCountDown.countDown();
                    } catch (OnyxException e) {
                        wrapper.setException(e);
                    }

                });

            }

            try {
                partitionScanCountDown.await();
            } catch (InterruptedException ignore) {
            }

            if (wrapper.getException() != null) {
                throw wrapper.getException();
            }
        }

        // Since 1.2.3 Added a fix for querying upon a relationship partition that is not ALL
        else if (getQuery().getPartition() != null) {
            final EntityDescriptor partitionDescriptor = getContext().getDescriptorForEntity(getQuery().getEntityType(), getQuery().getPartition());

            final MapBuilder dataFile = getContext().getDataFile(partitionDescriptor);
            DiskMap recs = (DiskMap) dataFile.getHashMap(partitionDescriptor.getEntityClass().getName(), partitionDescriptor.getIdentifier().getLoadFactor());

            // Get the partition ID
            IManagedEntity temp;
            try {
                temp = (IManagedEntity) ReflectionUtil.instantiate(getDescriptor().getEntityClass());
            } catch (IllegalAccessException | InstantiationException e) {
                throw new InvalidConstructorException(InvalidConstructorException.CONSTRUCTOR_NOT_FOUND, e);
            }
            PartitionHelper.setPartitionValueForEntity(temp, getQuery().getPartition(), getContext());

            Map partitionResults = scanPartition(recs, getPartitionId(temp));
            results.putAll(partitionResults);
        } else {
            return super.scan();
        }

        return results;
    }

}