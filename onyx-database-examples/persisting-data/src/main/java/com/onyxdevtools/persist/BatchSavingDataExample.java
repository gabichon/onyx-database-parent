package com.onyxdevtools.persist;

import com.onyx.exception.OnyxException;
import com.onyx.persistence.factory.PersistenceManagerFactory;
import com.onyx.persistence.factory.impl.EmbeddedPersistenceManagerFactory;
import com.onyx.persistence.manager.PersistenceManager;
import com.onyx.persistence.query.Query;
import com.onyxdevtools.persist.entities.RandomNumber;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * @author Chris Osborn
 */
@SuppressWarnings("WeakerAccess")
public class BatchSavingDataExample {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws OnyxException {

        String pathToOnyxDB = System.getProperty("user.home")
                + File.separatorChar + ".onyxdb"
                + File.separatorChar + "sandbox"
                + File.separatorChar + "persisting-data.oxd";

        PersistenceManagerFactory factory = new EmbeddedPersistenceManagerFactory(pathToOnyxDB);
        factory.setCredentials("username", "password");
        factory.initialize();

        PersistenceManager manager = factory.getPersistenceManager();

        Random randomNumberGenerator = new Random();

        List<RandomNumber> numbers = new LinkedList<>();

        for (int i = 0; i < 1000; i++) {
            RandomNumber number = new RandomNumber();
            number.setNumber(randomNumberGenerator.nextInt(100));
            numbers.add(number);
        }

        manager.saveEntities(numbers);

        Query query = new Query();
        query.setEntityType(RandomNumber.class);
        List<RandomNumber> savedNumbers = manager.executeQuery(query);

        System.out.println(savedNumbers.size() + " random numbers saved");

        factory.close(); //Close the embedded database after you're done with it

    }
}
