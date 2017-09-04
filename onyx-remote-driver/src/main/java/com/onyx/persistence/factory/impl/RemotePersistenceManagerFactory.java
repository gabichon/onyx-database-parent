package com.onyx.persistence.factory.impl;

import com.onyx.client.SSLPeer;
import com.onyx.client.auth.AuthenticationManager;
import com.onyx.client.exception.ConnectionFailedException;
import com.onyx.client.rmi.OnyxRMIClient;
import com.onyx.entity.SystemEntity;
import com.onyx.exception.EntityException;
import com.onyx.exception.InitializationException;
import com.onyx.persistence.context.SchemaContext;
import com.onyx.persistence.context.impl.DefaultSchemaContext;
import com.onyx.persistence.context.impl.RemoteSchemaContext;
import com.onyx.persistence.factory.PersistenceManagerFactory;
import com.onyx.persistence.manager.PersistenceManager;
import com.onyx.persistence.manager.impl.EmbeddedPersistenceManager;
import com.onyx.persistence.manager.impl.RemotePersistenceManager;
import com.onyx.persistence.query.Query;
import com.onyx.persistence.query.QueryCriteria;
import com.onyx.persistence.query.QueryCriteriaOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistence manager factory for an remote Onyx Database
 *
 * This is responsible for configuring a database connections to an external database.
 *
 * @author Tim Osborn
 * @since 1.0.0
 *
 * <pre>
 * <code>
 *
 *   PersistenceManagerFactory factory = new RemotePersistenceManagerFactory();
 *   factory.setCredentials("username", "password");
 *   factory.setLocation("onx://23.234.13.33:8080");
 *   factory.initialize();
 *
 *   PersistenceManager manager = factory.getPersistenceManager();
 *
 *   factory.close(); //Close the in memory database
 *
 * </code>
 * </pre>
 *
 * @see com.onyx.persistence.factory.PersistenceManagerFactory
 *
 * Tim Osborn, 02/13/2017 - This was augmented to use the new RMI Socket Server.  It has since been optimized
 */
public class RemotePersistenceManagerFactory extends EmbeddedPersistenceManagerFactory implements PersistenceManagerFactory, SSLPeer {

    private static final String PERSISTENCE_MANAGER_SERVICE = "1";
    private static final String AUTHENTICATION_MANAGER_SERVICE = "2";

    private OnyxRMIClient onyxRMIClient = new OnyxRMIClient();

    /**
     * Default Constructor
     *
     * @param databaseLocation Remote URI for database starting with onx://
     * @since 1.0.0
     */
    @SuppressWarnings("unused")
    public RemotePersistenceManagerFactory(String databaseLocation)
    {
        this(databaseLocation, databaseLocation);
    }

    /**
     * Default Constructor
     *
     * @param instance Cluster Instance unique identifier
     * @since 1.0.0
     */
    @SuppressWarnings("unused")
    public RemotePersistenceManagerFactory(String databaseLocation, String instance)
    {
        super(databaseLocation, instance);
        this.setSchemaContext(new RemoteSchemaContext(instance));
    }


    private PersistenceManager persistenceManager;

    /**
     * Getter for persistence manager.  Modified in 1.1.0 to keep a connection open.  If the connection is somehow
     * closed, this will automatically re-open it.
     *
     * @since 1.0.0
     * @return Instantiated Persistence Manager
     */
    @NotNull
    public PersistenceManager getPersistenceManager() {

        if (persistenceManager == null)
        {
            createPersistenceManager();
        }

        return persistenceManager;
    }


    /**
     * Helper method to instantiate and configure the persistence manager
     */
    private void createPersistenceManager()
    {
        if (this.getSchemaContext() == null) {
            setSchemaContext(new RemoteSchemaContext(getInstance()));
        }
        PersistenceManager proxy = (PersistenceManager) onyxRMIClient.getRemoteObject(PERSISTENCE_MANAGER_SERVICE, PersistenceManager.class);
        this.persistenceManager = new RemotePersistenceManager(proxy, onyxRMIClient);
        this.persistenceManager.setContext(getSchemaContext());

        DefaultSchemaContext.registeredSchemaContexts.put(getInstance(), getSchemaContext());

        final EmbeddedPersistenceManager systemPersistenceManager;

        // Since the connection remains persistent and open, we do not want to reset the system persistence manager.  That should have
        // remained open and valid through any network blip.
        if (getSchemaContext().getSystemPersistenceManager() == null) {
            systemPersistenceManager = new EmbeddedPersistenceManager();
            systemPersistenceManager.setContext(getSchemaContext());
            getSchemaContext().setSystemPersistenceManager(systemPersistenceManager);
        }

        ((RemoteSchemaContext) getSchemaContext()).setDefaultRemotePersistenceManager(persistenceManager);
    }

    /**
     * Connect to the remote database server
     *
     * @since 1.1.0
     * @throws InitializationException Exception occurred while connecting
     */
    private void connect() throws InitializationException
    {
        String location = getDatabaseLocation().replaceFirst("onx://", "");
        String[] locationParts = location.split(":");

        String port = locationParts[locationParts.length-1];
        String host = location.replace(":"+port, "");

        onyxRMIClient.setSslTrustStoreFilePath(this.sslTrustStoreFilePath);
        onyxRMIClient.setSslTrustStorePassword(this.sslTrustStorePassword);
        onyxRMIClient.setSslKeystoreFilePath(this.sslKeystoreFilePath);
        onyxRMIClient.setSslKeystorePassword(this.sslKeystorePassword);
        onyxRMIClient.setSslStorePassword(this.sslStorePassword);
        onyxRMIClient.setCredentials(this.getUser(), this.getPassword());
        AuthenticationManager authenticationManager = (AuthenticationManager) onyxRMIClient.getRemoteObject(AUTHENTICATION_MANAGER_SERVICE, AuthenticationManager.class);
        onyxRMIClient.setAuthenticationManager(authenticationManager);

        try {
            onyxRMIClient.connect(host, Integer.valueOf(port));
        } catch (ConnectionFailedException e) {
            this.close();
            throw new InitializationException(InitializationException.CONNECTION_EXCEPTION);
        }
    }

    /**
     * Initialize the database connection
     *
     * @since 1.0.0
     * @throws InitializationException Failure to start database due to either invalid credentials invalid network connection
     */
    @Override
    public void initialize() throws InitializationException
    {
        connect();

        try
        {
            Query query = new Query(SystemEntity.class, new QueryCriteria("name", QueryCriteriaOperator.NOT_EQUAL, ""));
            getPersistenceManager().executeQuery(query);
            getSchemaContext().start();
        } catch (EntityException e)
        {
            throw new InitializationException(InitializationException.INVALID_CREDENTIALS);
        }
    }

    /**
     * Safe shutdown of database connection
     * @since 1.0.0
     */
    @Override
    public void close()
    {
        onyxRMIClient.close();

        if(getSchemaContext() != null) {
            getSchemaContext().shutdown();
        }
        persistenceManager = null;
        setSchemaContext(null);
    }

    // SSL Protocol
    private String protocol = "TLSv1.2";

    // Keystore Password
    private String sslStorePassword;

    // Keystore file path
    private String sslKeystoreFilePath;

    // Keystore Password
    private String sslKeystorePassword;

    // Trust Store file path
    private String sslTrustStoreFilePath;

    // Trust store password.  This is typically the same as keystore Password
    private String sslTrustStorePassword;

    /**
     * Set for SSL Store Password.  Note, this is different than Keystore Password
     * @param sslStorePassword Password for SSL Store
     * @since 1.2.0
     */
    public void setSslStorePassword(String sslStorePassword) {
        this.sslStorePassword = sslStorePassword;
    }

    /**
     * Set Keystore file path.  This should contain the location of the JKS Keystore file
     * @param sslKeystoreFilePath Resource location of the JKS keystore
     * @since 1.2.0
     */
    public void setSslKeystoreFilePath(String sslKeystoreFilePath) {
        this.sslKeystoreFilePath = sslKeystoreFilePath;
    }

    /**
     * Set for SSL KeysStore Password.
     * @param sslKeystorePassword Password for SSL KEY Store
     * @since 1.2.0
     */
    public void setSslKeystorePassword(String sslKeystorePassword) {
        this.sslKeystorePassword = sslKeystorePassword;
    }

    /**
     * Set Trust store file path.  Location of the trust store JKS File.  This should contain
     * a file of the trusted sites that can access your secure endpoint
     * @param sslTrustStoreFilePath File path for JKS trust store
     */
    @SuppressWarnings("unused")
    public void setSslTrustStoreFilePath(String sslTrustStoreFilePath) {
        this.sslTrustStoreFilePath = sslTrustStoreFilePath;
    }

    /**
     * Trust store password
     * @param sslTrustStorePassword Password used to access your JKS Trust store
     */
    @SuppressWarnings("unused")
    public void setSslTrustStorePassword(String sslTrustStorePassword) {
        this.sslTrustStorePassword = sslTrustStorePassword;
    }

    /**
     * Getter for SSL Protocol.  By default this is TLSv1.2
     * @return Protocol used for SSL
     * @since 1.2.0
     */
    @SuppressWarnings("unused")
    public String getProtocol() {
        return protocol;
    }

    /**
     * Set Protocol for SSL
     * @param protocol Protocol used
     * @since 1.2.0
     */
    @SuppressWarnings("unused")
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    @Nullable
    @Override
    public String getSslStorePassword() {
        return sslStorePassword;
    }

    @Nullable
    @Override
    public String getSslKeystoreFilePath() {
        return sslKeystoreFilePath;
    }

    @NotNull
    @Override
    public String getSslKeystorePassword() {
        return sslKeystorePassword;
    }

    @NotNull
    @Override
    public String getSslTrustStoreFilePath() {
        return sslTrustStoreFilePath;
    }

    @NotNull
    @Override
    public String getSslTrustStorePassword() {
        return sslTrustStorePassword;
    }

}
