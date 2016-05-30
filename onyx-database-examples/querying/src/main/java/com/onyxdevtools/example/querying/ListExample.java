package com.onyxdevtools.example.querying;

import com.onyx.exception.EntityException;
import com.onyx.exception.InitializationException;

import com.onyx.persistence.factory.PersistenceManagerFactory;
import com.onyx.persistence.factory.impl.EmbeddedPersistenceManagerFactory;
import com.onyx.persistence.manager.PersistenceManager;
import com.onyx.persistence.query.QueryCriteria;
import com.onyx.persistence.query.QueryCriteriaOperator;

import com.onyxdevtools.example.querying.entities.Player;

import java.io.File;
import java.io.IOException;

import java.util.List;


/**
 @author  cosborn
 */
public class ListExample
{
    public ListExample()
    {
    }

    public static void demo() throws InitializationException, EntityException, IOException
    {
        // get an instance of the persistenceManager
        final PersistenceManagerFactory factory = new EmbeddedPersistenceManagerFactory();
        factory.setCredentials("onyx-user", "SavingDataIsFun!");

        final String pathToOnyxDB = System.getProperty("user.home") + File.separatorChar + ".onyxdb" + File.separatorChar + "sandbox" +
            File.separatorChar + "querying-db.oxd";
        factory.setDatabaseLocation(pathToOnyxDB);
        factory.initialize();

        final PersistenceManager manager = factory.getPersistenceManager();

        final QueryCriteria criteria = new QueryCriteria("playerId", QueryCriteriaOperator.GREATER_THAN, 0);

        final List<Player> players = manager.list(Player.class, criteria);

        for (final Player player : players)
        {
            System.out.println(player.getLastName() + ", " + player.getFirstName());
        }

        factory.close(); // close the factory so that we can use it again

    }
}
