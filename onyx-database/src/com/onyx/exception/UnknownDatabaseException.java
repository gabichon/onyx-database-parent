package com.onyx.exception;

/**
 * Created by tosborn1 on 12/31/15.
 */
public class UnknownDatabaseException extends EntityException
{
    protected String cause = null;

    @SuppressWarnings("unused")
    public UnknownDatabaseException()
    {

    }

    public UnknownDatabaseException(Exception e)
    {
        this.cause = e.getMessage();
    }
}
