package com.onyx.cli

import com.onyx.application.impl.DatabaseServer
import org.apache.commons.cli.*

/**
 * Created by tosborn1 on 2/13/17.
 *
 * This is responsible for parsing the command line options of a database server
 */
open class CommandLineParser(args: Array<String>) {

    protected val commandLineArguments: CommandLine by lazy {  parseCommandLine(args) }

    /**
     * Configure Command Line Options for Data Server
     *
     * @return CLI Options
     * @since 1.0.0
     */
    private fun configureCommandLineOptions(): Options {
        // create the Options
        val options = Options()

        options.addOption("P", OPTION_PORT, true, "Server port number")
        options.addOption("u", OPTION_USER, true, "Database username")
        options.addOption("p", OPTION_PASSWORD, true, "Database password")
        options.addOption("l", OPTION_LOCATION, true, "Database filesystem databaseLocation")
        options.addOption("i", OPTION_INSTANCE, true, "Database instance name")
        options.addOption("k", OPTION_KEYSTORE, true, "Keystore file path.")
        options.addOption("t", OPTION_TRUST_STORE, true, "Trust Store file path.")
        options.addOption("sp", OPTION_STORE_PASSWORD, true, "SSL Store Password.")
        options.addOption("kp", OPTION_KEYSTORE_PASSWORD, true, "Keystore password. ")
        options.addOption("tp", OPTION_TRUST_STORE_PASSWORD, true, "Trust Store password.")
        options.addOption("h", OPTION_HELP, false, "Help")

        return options
    }

    /**
     * Parse command line arguments
     * @param args arguments to parse
     * @return CommandLine object
     */
    protected open fun parseCommandLine(args: Array<String>): CommandLine {
        // create the command line parser
        val parser = DefaultParser()
        val options = configureCommandLineOptions()

        val commandLine: CommandLine
        try {
            // parse the command line arguments
            commandLine = parser.parse(options, args)
        } catch (exp: ParseException) {
            // oops, something went wrong
            System.err.println("Invalid Arguments.  Reason: " + exp.message)
            throw RuntimeException(exp)
        }

        if (commandLine.hasOption(OPTION_HELP)) {
            val formatter = HelpFormatter()
            formatter.printHelp("onyx", options)
            System.exit(0)
        }

        if (!commandLine.hasOption(OPTION_LOCATION)) {
            System.err.println("Invalid Database Location.  Location is required!")
            throw RuntimeException()
        }

        return commandLine
    }

    /**
     * Configure Database With Command Line Options.  With V1.2.0, we removed the useSSL since it is redundant.
     *
     * @param databaseServer - Database Server instance
     * @since 1.0.0
     */
    open fun configureDatabaseWithCommandLineOptions(databaseServer: DatabaseServer) {
        if (commandLineArguments.hasOption(OPTION_USER) && commandLineArguments.hasOption(OPTION_PASSWORD))
            databaseServer.setCredentials(commandLineArguments.getOptionValue(OPTION_USER), commandLineArguments.getOptionValue(OPTION_PASSWORD))
        else
            databaseServer.setCredentials("admin", "admin")
        if (commandLineArguments.hasOption(OPTION_PORT))
            databaseServer.port = Integer.valueOf(commandLineArguments.getOptionValue(OPTION_PORT))
        if (commandLineArguments.hasOption(OPTION_INSTANCE))
            databaseServer.instance = commandLineArguments.getOptionValue(OPTION_INSTANCE)
        if (commandLineArguments.hasOption(OPTION_KEYSTORE))
            databaseServer.sslKeystoreFilePath = commandLineArguments.getOptionValue(OPTION_KEYSTORE)
        if (commandLineArguments.hasOption(OPTION_TRUST_STORE))
            databaseServer.sslTrustStoreFilePath = commandLineArguments.getOptionValue(OPTION_TRUST_STORE)
        if (commandLineArguments.hasOption(OPTION_KEYSTORE_PASSWORD))
            databaseServer.sslKeystorePassword = commandLineArguments.getOptionValue(OPTION_KEYSTORE_PASSWORD)
        if (commandLineArguments.hasOption(OPTION_STORE_PASSWORD))
            databaseServer.sslStorePassword = commandLineArguments.getOptionValue(OPTION_STORE_PASSWORD)
        if (commandLineArguments.hasOption(OPTION_TRUST_STORE_PASSWORD))
            databaseServer.sslTrustStorePassword = commandLineArguments.getOptionValue(OPTION_TRUST_STORE_PASSWORD)
    }

    val databaseLocation: String
        get() = commandLineArguments.getOptionValue(OPTION_LOCATION)

    companion object {

        // Command Line Options
        protected val OPTION_PORT = "port"
        private val OPTION_USER = "user"
        private val OPTION_PASSWORD = "password"
        private val OPTION_LOCATION = "databaseLocation"
        private val OPTION_INSTANCE = "instance"
        private val OPTION_KEYSTORE = "keystore"
        private val OPTION_TRUST_STORE = "trust-store"
        private val OPTION_STORE_PASSWORD = "store-password"
        private val OPTION_KEYSTORE_PASSWORD = "keystore-password"
        private val OPTION_TRUST_STORE_PASSWORD = "trust-password"
        private val OPTION_HELP = "help"
    }
}