package embedded.exception;


import category.EmbeddedDatabaseTests;
import com.onyx.exception.*;
import com.onyx.persistence.query.Query;
import com.onyx.persistence.query.QueryCriteria;
import com.onyx.persistence.query.QueryCriteriaOperator;
import com.onyx.persistence.query.AttributeUpdate;
import embedded.base.BaseTest;
import entities.ValidationEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

/**
 * Created by timothy.osborn on 1/21/15.
 * Updated by Chris Osborn on 5/15/15
 */
@Category({ EmbeddedDatabaseTests.class })
public class TestQueryValidation extends BaseTest
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
    public void testNullValue() throws OnyxException
    {
        Query updateQuery = new Query(ValidationEntity.class, new QueryCriteria("id", QueryCriteriaOperator.EQUAL, 3l), new AttributeUpdate("requiredString", null));
        manager.executeUpdate(updateQuery);
    }


    @Test(expected = AttributeMissingException.class)
    public void testAttributeMissing() throws OnyxException
    {
        Query updateQuery = new Query(ValidationEntity.class, new QueryCriteria("id", QueryCriteriaOperator.EQUAL, 3l), new AttributeUpdate("booger", null));
        manager.executeUpdate(updateQuery);
    }

    @Test(expected = AttributeSizeException.class)
    public void testAttributeSizeException() throws OnyxException
    {
        Query updateQuery = new Query(ValidationEntity.class, new QueryCriteria("id", QueryCriteriaOperator.EQUAL, 3l), new AttributeUpdate("maxSizeString", "12345678901"));
        manager.executeUpdate(updateQuery);

    }

    @Test(expected = AttributeUpdateException.class)
    public void testUpdateIdentifier() throws OnyxException
    {
        Query updateQuery = new Query(ValidationEntity.class, new QueryCriteria("id", QueryCriteriaOperator.EQUAL, 3l), new AttributeUpdate("id", 5l));
        manager.executeUpdate(updateQuery);
    }

    @Test(expected = AttributeTypeMismatchException.class)
    public void testTypeMisMatchException() throws OnyxException
    {
        Query updateQuery = new Query(ValidationEntity.class, new QueryCriteria("id", QueryCriteriaOperator.EQUAL, 3l), new AttributeUpdate("requiredString", 5l));
        manager.executeUpdate(updateQuery);
    }
    
    @Test(expected = OnyxException.class)
    public void testMissingEntityTypeException() throws OnyxException
    {
        QueryCriteria criteria = new QueryCriteria("name", QueryCriteriaOperator.NOT_NULL);
        Query query = new Query();
        // query.setEntityType(SystemEntity.class); //should throw error because this line is missing
        query.setCriteria(criteria);
        manager.executeQuery(query);
    }

}
