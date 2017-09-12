package com.onyx.fetch.impl;

import com.onyx.descriptor.EntityDescriptor;
import com.onyx.descriptor.RelationshipDescriptor;
import com.onyx.diskmap.DiskMap;
import com.onyx.diskmap.MapBuilder;
import com.onyx.diskmap.node.SkipListNode;
import com.onyx.exception.OnyxException;
import com.onyx.exception.InvalidConstructorException;
import com.onyx.exception.InvalidQueryException;
import com.onyx.fetch.PartitionReference;
import com.onyx.fetch.ScannerFactory;
import com.onyx.fetch.TableScanner;
import com.onyx.helpers.PartitionHelper;
import com.onyx.persistence.IManagedEntity;
import com.onyx.persistence.context.SchemaContext;
import com.onyx.persistence.manager.PersistenceManager;
import com.onyx.persistence.query.Query;
import com.onyx.persistence.query.QueryCriteria;
import com.onyx.persistence.query.QueryPartitionMode;
import com.onyx.interactors.record.RecordInteractor;
import com.onyx.relationship.RelationshipController;
import com.onyx.relationship.RelationshipReference;
import com.onyx.util.ReflectionUtil;
import com.onyx.util.map.CompatHashMap;

import java.util.*;

/**
 * Created by timothy.osborn on 1/3/15.
 *
 * Scan relationships for matching criteria
 */
public class RelationshipScanner extends AbstractTableScanner implements TableScanner {

    @SuppressWarnings("WeakerAccess")
    protected RelationshipDescriptor relationshipDescriptor;

    /**
     * Constructor
     *
     * @param criteria Query Criteria
     * @param classToScan Class type to scan
     * @param descriptor Entity descriptor of entity type to scan
     */
    public RelationshipScanner(QueryCriteria criteria, Class classToScan, EntityDescriptor descriptor, MapBuilder temporaryDataFile, Query query, SchemaContext context, PersistenceManager persistenceManager) throws OnyxException
    {
        super(criteria, classToScan, descriptor, temporaryDataFile, query, context, persistenceManager);
    }

    /**
     * Full Table get all relationships
     *
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, Long> scan() throws OnyxException
    {

        Map startingPoint = new HashMap();

        // We do not support querying relationships by all partitions.
        // This would be about the most rediculous non optimized query
        // and is frowned upon
        if (this.query.getPartition() == QueryPartitionMode.ALL)
        {
            throw new InvalidQueryException();
        }

        // Added the ability to start with a partition
        if (this.descriptor.getPartition() != null) {
            // Get the partition ID
            IManagedEntity temp;
            try {
                temp = (IManagedEntity) ReflectionUtil.instantiate(descriptor.getEntityClass());
            } catch (IllegalAccessException | InstantiationException e) {
                throw new InvalidConstructorException(InvalidConstructorException.CONSTRUCTOR_NOT_FOUND, e);
            }
            PartitionHelper.setPartitionValueForEntity(temp, query.getPartition(), getContext());
            long partitionId = getPartitionId(temp);

            for (SkipListNode reference : (Set<SkipListNode>) ((DiskMap) records).referenceSet()) {
                startingPoint.put(new PartitionReference(partitionId, reference.recordId), new PartitionReference(partitionId, reference.recordId));
            }
        } else {
            // Hydrate the entire reference set of parent entity before scanning the relationship
            for (SkipListNode reference : (Set<SkipListNode>) ((DiskMap) records).referenceSet()) {
                startingPoint.put(reference.recordId, reference.recordId);
            }
        }

        return scan(startingPoint);
    }

    /**
     * Full Scan with existing values
     *
     * @param existingValues Existing values to check criteria
     * @return filterd map of results matching additional criteria
     * @throws OnyxException Cannot scan relationship values
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map scan(Map existingValues) throws OnyxException
    {
        // Retain the original attribute
        final String originalAttribute = criteria.getAttribute();

        // Get the attribute name.  If it has multiple tokens, that means it is another relationship.
        // If that is the case, we gotta find that one
        final String[] segments = originalAttribute.split("\\.");

        // Map <ChildIndex, ParentIndex> // Inverted list so we can use it to scan using an normal full table scanner or index scanner
        final Map relationshipIndexes = getRelationshipIndexes(segments[0], existingValues);
        final Map returnValue = new CompatHashMap();

        // We are going to set the attribute name so we can continue going down the chain.  We are going to remove the
        // processed token through
        criteria.setAttribute(criteria.getAttribute().replaceFirst(segments[0] + "\\.", ""));

        // Get the next scanner because we are not at the end of the line.  Otherwise, we would not have gotten to this place
        final TableScanner tableScanner = ScannerFactory.getInstance(getContext()).getScannerForQueryCriteria(criteria, relationshipDescriptor.getInverseClass(), temporaryDataFile, query, persistenceManager);

        // Sweet, lets get the scanner.  Note, this very well can be recursive, but sooner or later it will get to the
        // other scanners
        final Map childIndexes = tableScanner.scan(relationshipIndexes);

        // Swap parent / child after getting results.  This is because we can use the child when hydrating stuff
        for (Object childIndex : childIndexes.keySet())
        {
            // Gets the parent
            returnValue.put(relationshipIndexes.get(childIndex), childIndex);
        }

        criteria.setAttribute(originalAttribute);

        return returnValue;
    }

    /**
     * Get Relationship Indexes
     *
     * @param attribute Attribute match
     * @param existingValues Existing values to check
     * @return References that match criteria
     */
    @SuppressWarnings("unchecked")
    private Map getRelationshipIndexes(String attribute, Map existingValues) throws OnyxException
    {
        final Map allResults = new CompatHashMap();

        final Iterator iterator = existingValues.keySet().iterator();

        if (this.query.getPartition() == QueryPartitionMode.ALL) {
            throw new InvalidQueryException();
        }

        relationshipDescriptor = this.descriptor.getRelationships().get(attribute);
        final RelationshipController relationshipController = getContext().getRelationshipController(relationshipDescriptor);
        final RecordInteractor inverseRecordInteractor = getDefaultInverseRecordInteractor();

        List<RelationshipReference> relationshipIdentifiers;
        Object keyValue;

        while(iterator.hasNext())
        {
            if(query.isTerminated())
                return allResults;

            keyValue = iterator.next();

            if(keyValue instanceof PartitionReference)
            {
                relationshipIdentifiers = relationshipController.getRelationshipIdentifiersWithReferenceId((PartitionReference)keyValue);
            }
            else
            {
                relationshipIdentifiers = relationshipController.getRelationshipIdentifiersWithReferenceId((long)keyValue);
            }
            for(RelationshipReference id : relationshipIdentifiers)
            {

                if(id.partitionId == 0)
                {
                    allResults.put(inverseRecordInteractor.getReferenceId(id.identifier), keyValue);
                }
                else
                {
                    RecordInteractor recordInteractorForPartition = getRecordInteractorForPartition(id.partitionId);
                    PartitionReference reference = new PartitionReference(id.partitionId, recordInteractorForPartition.getReferenceId(id.identifier));
                    allResults.put(reference, keyValue);
                }

            }
        }

        return allResults;
    }

    /**
     * Grabs the inverse record controller
     *
     * @return Record controller for inverse relationship
     * @throws OnyxException Cannot get record controller for inverse relationship
     */
    private RecordInteractor getDefaultInverseRecordInteractor() throws OnyxException
    {
        final EntityDescriptor inverseDescriptor = getContext().getBaseDescriptorForEntity(relationshipDescriptor.getInverseClass());
        return getContext().getRecordInteractor(inverseDescriptor);
    }
}
