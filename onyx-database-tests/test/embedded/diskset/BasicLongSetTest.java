package embedded.diskset;

import category.EmbeddedDatabaseTests;
import com.onyx.structure.DefaultMapBuilder;
import com.onyx.structure.MapBuilder;
import entities.EntityYo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by tosborn on 3/21/15.
 */
@Category({ EmbeddedDatabaseTests.class })
public class BasicLongSetTest extends AbstractTest
{

    @Test
    public void addTest()
    {
        MapBuilder store = new DefaultMapBuilder(TEST_DATABASE);

        Set mySet = store.newLongSet();
        mySet.add(222l);
        mySet.add(827323l);
        mySet.add(3234l);

        assert mySet.contains(827323l);
        assert mySet.contains(222l);
        assert mySet.contains(3234l);

        store.close();
    }

    @Test
    public void iteratorTest()
    {
        MapBuilder store = new DefaultMapBuilder(TEST_DATABASE);

        Set mySet = store.newLongSet();
        mySet.add(222l);
        mySet.add(827323l);
        mySet.add(3234l);

        Set copySet = new HashSet<>();
        copySet.add(222l);
        copySet.add(827323l);
        copySet.add(3234l);
        int i = 0;
        Iterator iterator = mySet.iterator();
        while (iterator.hasNext())
        {
            long value = (long)iterator.next();
            copySet.remove(value);
            i++;
        }

        assert i == 3;
        assert copySet.size() == 0;
        store.close();
    }

    @Test
    public void removeTest()
    {
        MapBuilder store = new DefaultMapBuilder(TEST_DATABASE);

        Set mySet = store.newLongSet();
        mySet.add(827323l);
        mySet.add(222l);
        mySet.add(3234l);

        assert mySet.remove(222l);
        assert mySet.contains(827323l);
        assert mySet.contains(3234l);
        assert !mySet.contains(222l);
        store.close();
    }



    @Test
    public void jumboTest()
    {
        MapBuilder store = new DefaultMapBuilder(TEST_DATABASE);
        Set mySet = store.newLongSet();

        System.out.println("Starting Jumbo");
        long time = System.currentTimeMillis();
        for(int i = 0; i < 500000; i++)
        {
            mySet.add((long)i);
        }

        store.commit();

        System.out.println("Took " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        for(int i = 1; i < 500000; i++)
        {
            assert mySet.contains((long)i);
        }


        System.out.println("Took " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        Iterator it = mySet.iterator();
        int i = 0;
        while(it.hasNext())
        {
            it.next();
            i++;
        }
        System.out.println("Took " + (System.currentTimeMillis() - time) + " for this many " + i);
        store.close();

        System.out.println("Done with Jumbo");

    }

    @Test
    public void updateTest()
    {
        MapBuilder store = new DefaultMapBuilder(TEST_DATABASE);
        Map<Integer, Object> myMap = store.getHashMap("second");

        long time = System.currentTimeMillis();
        for(int i = 0; i < 100000; i++)
        {
            myMap.put(new Integer(i), "Hiya");
        }

        System.out.println("Took " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        for(int i = 5000; i < 10000; i++)
        {
            myMap.remove(new Integer(i));
        }

        for(int i = 0; i < 5000; i++)
        {
            String value = (String)myMap.get(new Integer(i));
            Assert.assertTrue(value.equals("Hiya"));
        }

        for(int i = 3000; i < 6000; i++)
        {
            myMap.put(new Integer(i), "Wheee woooooo haaaa" + i);
        }

        for(int i = 6000; i < 10000; i++)
        {
            String value = (String)myMap.get(new Integer(i));
            Assert.assertTrue(value == null);
        }

        for(int i = 3000; i < 6000; i++)
        {

            String value = (String)myMap.get(new Integer(i));
            Assert.assertTrue(value.equals("Wheee woooooo haaaa" + i));
        }

        System.out.println("Took " + (System.currentTimeMillis() - time));
        store.close();
    }


}