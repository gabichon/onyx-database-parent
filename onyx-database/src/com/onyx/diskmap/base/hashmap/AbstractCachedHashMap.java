package com.onyx.diskmap.base.hashmap;

import com.onyx.diskmap.base.concurrent.ConcurrentWeakHashMap;
import com.onyx.diskmap.node.Header;
import com.onyx.diskmap.store.Store;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Created by tosborn1 on 2/15/17.
 *
 * This class controls the caching of a Hash Map within a disk store
 *
 * @since 1.2.0
 */
public abstract class AbstractCachedHashMap<K,V> extends AbstractHashMap<K,V> {

    private Map<Integer, Long> cache;
    private Map<Integer, Integer> mapCache;

    /**
     * Constructor
     *
     * @param fileStore File storage
     * @param header Head of the data structore
     * @param headless Whether it is used in a stateless manner.
     * @param loadFactor Indicates how large to allocate the initial data structure
     *
     * @since 1.2.0
     */
    protected AbstractCachedHashMap(Store fileStore, Header header, boolean headless, int loadFactor) {
        super(fileStore, header, headless, loadFactor);
        cache = new ConcurrentWeakHashMap<>();
        mapCache = new ConcurrentWeakHashMap<>();
    }

    /**
     * Insert reference into the hash array.  This will add it to a cache before writing it to the store.
     *
     * @param hash The maximum hash value can only contain as many digits as the size of the loadFactor
     * @param reference Reference of the sub data structure to put it into.
     * @return The reference that was inserted
     *
     * @since 1.2.0
     */
    protected long insertReference(int hash, long reference)
    {
        cache.put(hash, reference);
        super.insertReference(hash, reference);
        return reference;
    }

    /**
     * Update the reference of the hash.
     *
     * @param hash Identifier of the data structure
     * @param reference Reference of the sub data structure to update to.
     * @since 1.2.0
     * @return The reference that was sent in.
     */
    protected long updateReference(int hash, long reference)
    {
        cache.put(hash, reference);
        super.updateReference(hash, reference);
        return reference;
    }

    /**
     * Get the sub data structure reference for the hash id.
     * @param hash Identifier of the data structure
     * @return Location of the data structure within the volume/store
     *
     * @since 1.2.0
     */
    protected long getReference(int hash)
    {
        return cache.compute(hash, (integer, aLong) -> {
            if(aLong == null)
                aLong = AbstractCachedHashMap.super.getReference(hash);
            return aLong;
        });
    }

    /**
     * Add iteration list.  This method adds a reference so that the iterator knows what to iterate through without
     * guessing wich element within the hash as a sub data structure reference.
     *
     * @param buffer Byte Buffer to add the hash id to.
     * @param hash Identifier of the sub data structure
     * @param count The current size of the hash table
     *
     * @since 1.2.0
     */
    void addIterationList(ByteBuffer buffer, int hash, int count)
    {
        mapCache.put(count, hash);
        super.addIterationList(buffer, hash, count);
    }

    /**
     * Get Map ID within the iteration index
     * @param index Index within the list of maps
     * @return The hash identifier of the sub data structure
     * @since 1.2.0
     */
    protected int getMapIdentifier(int index) {
        return mapCache.compute(index, (integer, integer2) -> {
            if (integer2 != null)
                return integer2;
            return AbstractCachedHashMap.super.getMapIdentifier(index);
        });
    }

    /**
     * Clear the cache
     * @since 1.2.0
     */
    @Override
    public void clear()
    {
        super.clear();
        this.cache.clear();
        this.mapCache.clear();
    }

}
