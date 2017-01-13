package remote.exception;


import category.RemoteServerTests;
import com.onyx.exception.*;
import com.onyx.persistence.query.Query;
import com.onyx.persistence.query.QueryCriteria;
import com.onyx.persistence.query.QueryCriteriaOperator;
import com.onyx.persistence.update.AttributeUpdate;
import entities.ValidationEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import remote.base.RemoteBaseTest;

import java.io.IOException;

/**
 * Created by timothy.osborn on 1/21/15.
 */
@Category({ RemoteServerTests.class })
public class TestQueryValidation extends RemoteBaseTest
{

    @Before
    public void before() throws InitializationException
    {
        initialize();
    }

    @After
    public void after() throws IOException
    {
        shutdown();
    }

    @Test(expected = AttributeNonNullException.class)
    public void testNullValue() throws EntityException
    {
        Query updateQuery = new Query(ValidationEntity.class, new QueryCriteria("id", QueryCriteriaOperator.EQUAL, 3l), new AttributeUpdate<>("requiredString", QueryCriteria.NULL_STRING_VALUE));
        manager.executeUpdate(updateQuery);
    }


    @Test(expected = AttributeMissingException.class)
    public void testAttributeMissing() throws EntityException
    {
        Query updateQuery = new Query(ValidationEntity.class, new QueryCriteria("id", QueryCriteriaOperator.EQUAL, 3l), new AttributeUpdate<>("booger", QueryCriteria.NULL_STRING_VALUE));
        manager.executeUpdate(updateQuery);
    }

    @Test(expected = AttributeSizeException.class)
    public void testAttributeSizeException() throws EntityException
    {
        Query updateQuery = new Query(ValidationEntity.class, new QueryCriteria("id", QueryCriteriaOperator.EQUAL, 3l), new AttributeUpdate<>("maxSizeString", "12345678901"));
        manager.executeUpdate(updateQuery);

    }

    @Test(expected = AttributeUpdateException.class)
    public void testUpdateIdentifier() throws EntityException
    {
        Query updateQuery = new Query(ValidationEntity.class, new QueryCriteria("id", QueryCriteriaOperator.EQUAL, 3l), new AttributeUpdate<>("id", 5l));
        manager.executeUpdate(updateQuery);
    }

    @Test(expected = AttributeTypeMismatchException.class)
    public void testTypeMisMatchException() throws EntityException
    {
        Query updateQuery = new Query(ValidationEntity.class, new QueryCriteria("id", QueryCriteriaOperator.EQUAL, 3l), new AttributeUpdate<>("requiredString", 5l));
        manager.executeUpdate(updateQuery);
    }

}
