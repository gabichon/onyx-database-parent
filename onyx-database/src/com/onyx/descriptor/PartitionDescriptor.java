package com.onyx.descriptor;

/**
 * Created by timothy.osborn on 12/11/14.
 */
public class PartitionDescriptor extends AbstractBaseDescriptor
{
    protected String partitionValue = "";

    public String getPartitionValue()
    {
        return partitionValue;
    }

    public void setPartitionValue(String partitionValue)
    {
        this.partitionValue = partitionValue;
    }
}
