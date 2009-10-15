package org.dacapo.derby;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.AccessController;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.derbyTesting.junit.JDBCDataSource;
import org.apache.derbyTesting.system.oe.client.Display;
import org.apache.derbyTesting.system.oe.client.Load;
import org.apache.derbyTesting.system.oe.client.MultiThreadSubmitter;
import org.apache.derbyTesting.system.oe.client.Operations;
import org.apache.derbyTesting.system.oe.client.Submitter;
import org.apache.derbyTesting.system.oe.load.ThreadInsert;
import org.apache.derbyTesting.system.oe.util.OERandom;
import org.apache.derby.tools.ij;
import org.apache.derby.jdbc.EmbeddedDriver;

import org.dacapo.parser.Config;

public class TPCC
{

  // Transaction timeouts, note thats DEADLOCK_TIMEOUT < TRANSACTION_TIMEOUT
  // for deadlock detection to be meaningful. Times are in seconds.
  public final static String  PROP_DEADLOCK_TIMEOUT         = "derby.locks.deadlockTimeout";
  public final static String  PROP_TRANSACTION_TIMEOUT      = "derby.locks.waitTimeout";

  public final static String  DEADLOCK_TIMEOUT              = "4";
  public final static String  TRANSACTION_TIMEOUT           = "20";
  public final static int     RETRY_LIMIT                   = 5;

  // database
  private final static String URL_BASE                      = "jdbc:derby:";
  private final static String DATABASE_NAME                 = "testderbydb";
  private final static String USERNAME                      = "user";
  private final static String PASSWORD                      = "password";
  private final static String USER                          = "derby";
  private final static String PASS                          = "derby";

  // default configuration for external testing of derby
  //database scale (see TPC-C documentation) number of terminals (clients) that run transactions
  private final static int    DEF_NUM_OF_TERMINALS          = 2;
  // database scale (see TPC-C documentation)
  private final static short  DEF_SCALE                     = 1;                            
  // number of transactions each terminal (client) runs
  private final static int    DEF_TRANSACTIONS_PER_TERMINAL = 100;
  // number of warehoueses (see TPC-C documentation)
  private final static short  DEF_NUM_OF_WAREHOUSES         = 1;

  // Some defaults for running tests
  private final static String SMALL                         = "small";
  private static int          NUMBER_OF_ITERATIONS          = 2;

  // Basic configurable items
  // number of threads to perform loading of the database
  private int                 loaderThreads                 = 1;
  
  // this loaderThreads seems determininstic and should be
  // to the number of CPU cores
  private short               scale                         = DEF_SCALE;
  private int                 numberOfTerminals             = DEF_NUM_OF_TERMINALS;
  private int                 transactionsPerTerminal       = DEF_TRANSACTIONS_PER_TERMINAL;
  private short               numberOfWarehouses            = DEF_NUM_OF_WAREHOUSES;

  // Time for last iteration
  private long                iterationTime;

  // OLTP runners
  private Submitter[]         submitters;
  private Display[]           displays;
  private OERandom[]          rands;

  private Config              config;
  private String              size;
  private Driver              driver;
  private Properties          properties;
  private Connection          conn;

  // transaction failure configuration  
  private String              deadlockTimeout               = DEADLOCK_TIMEOUT;
  private String              transactionTimeout            = TRANSACTION_TIMEOUT;
  private int                 retryLimit                    = RETRY_LIMIT;
  
  // Location of the database, this specifies the directory (folder) on a file
  // system where the database is stored. This application must have be able to 
  // create this directory (folder)
  private File                database;

  // A random seed for initializing the database and the OLTP terminals.
  private final static long   SEED                          = 897523978813691l;
  private final static int    SEED_STEP                     = 100000;

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception
  {
    System.out.println("Started");

    TPCC m = make(null, new File("."));

    System.out.println("going to perform setup");

    m.prepare(SMALL);

    System.out.println("completed setup");

    for (int i = 1; i <= NUMBER_OF_ITERATIONS; i++)
    {
      System.out.println("performing preIteration " + i);
      m.preIteration(SMALL);
      System.out.println("performing iteration " + i);
      m.iteration(SMALL);
      System.out.println("performing postIteration " + i);
      m.postIteration(SMALL);
    }

    System.out.println("performing cleanup");

    m.cleanup();

    System.out.println("finished");
  }

  public static TPCC make(Config config, File scratch) throws Exception
  {
    return new TPCC(config, scratch);
  }

  public TPCC(Config config, File scratch) throws Exception
  {
    this.config = config;

    // seem to need to set this early and in the system properties
    System.setProperty(PROP_DEADLOCK_TIMEOUT, DEADLOCK_TIMEOUT);
    System.setProperty(PROP_TRANSACTION_TIMEOUT, TRANSACTION_TIMEOUT);

    driver = new EmbeddedDriver();
    properties = (Properties) System.getProperties().clone();

    // make the database relative to the scratch location
    database = new File(scratch, DATABASE_NAME);

    properties.setProperty(USERNAME, USER);
    properties.setProperty(PASSWORD, PASS);
  }

  public void prepare(String size) throws Exception
  {
    this.size = size;

    configure();

    // make a seeded random number generator for each submitter
    rands = new OERandom[numberOfTerminals];

    // for the moment we are not interested displaying the results of each
    // transaction so we leave each entry in the displays array as a null
    displays = new Display[numberOfTerminals];

    // create a set of Submitter each with a Standard operations implementation
    submitters = new Submitter[numberOfTerminals];

    // ensure any preserved database is removed
    deleteDatabase();

    // create database
    createDatabase();

    // create schema
    createSchema();

    // load data
    loadData();

    // create constraints (post load, may want to move this pre load)
    createConstraints();

    // close the connection for initializing the database
    closeConnection();
  }

  public void preIteration(String size) throws Exception
  {
    // we can't change size after the initial prepare(size)
    assert this.size.equalsIgnoreCase(size);

    // make sure we have the same seeds each run
    for (int i = 0; i < rands.length; i++)
    {
      rands[i] = new OERandom(SEED_STEP * i, SEED + SEED_STEP * i);
    }

    // create a Submitter for each thread, and then pass to
    // org.apache.derbyTesting.system.oe.client.MultiThreadSubmitter.multiRun
    for (int i = 0; i < submitters.length; i++)
    {
      Operations ops = new Operation(makeConnection(false), retryLimit);

      submitters[i] = new Submitter(null, ops, rands[i], numberOfWarehouses);
    }
    
    // clean up any hang-over from previous iterations
    System.gc();
  }

  public void iteration(String size) throws Exception
  {
    // we can't change size after the initial prepare(size)
    assert this.size.equalsIgnoreCase(size);

    iterationTime = MultiThreadSubmitter.multiRun(submitters, displays,
        transactionsPerTerminal);

    report();
  }

  public void postIteration(String size) throws Exception
  {
    // we can't change size after the initial prepare(size)
    assert this.size.equalsIgnoreCase(size);

    for (int i = 0; i < submitters.length; i++)
    {
      submitters[i] = null;
    }
  }

  public void cleanup() throws Exception
  {
    deleteDatabase();
  }

  // ----------------------------------------------------------------------------------
  private void report()
  {
    System.out.println("BEGIN: transaction count report");
    for (int i = 0; i < submitters.length; i++)
    {
      submitters[i].printReport(System.out);
    }
    System.out.println("END: transaction count report");
  }

  private void createDatabase() throws Exception
  {
    DataSource ds = JDBCDataSource.getDataSource(getDatabaseName());

    JDBCDataSource.setBeanProperty(ds, "createDatabase", "create");

    ds.getConnection().close();
  }

  private void createSchema() throws Exception
  {
    // create schema
    runScript("schema.sql");
    runScript("dataroutines.sql");
    runScript("delivery.sql");
  }

  private void createConstraints() throws Exception
  {
    // create key constraints
    runScript("primarykey.sql");
    runScript("foreignkey.sql");
    runScript("index.sql");
  }

  private void loadData() throws Exception
  {

    // Use simple insert statements to insert data.
    // currently only this form of load is present, once we have
    // different implementations, the loading mechanism will need
    // to be configurable taking an option from the command line
    // arguments.
    DataSource ds = JDBCDataSource.getDataSource(getDatabaseName());

    Load loader = new ThreadInsert(ds);
    loader.setSeed(SEED);
    loader.setupLoad(getConnection(), scale);
    if (loaderThreads > 0)
      loader.setThreadCount(loaderThreads);

    loader.populateAllTables();

    // Way to populate data is extensible. Any other implementation
    // of org.apache.derbyTesting.system.oe.client.Load can be used
    // to load data. configurable using the oe.load.insert property
    // that is defined in oe.properties
    // One extension would be to have an implementation that
    // uses bulkinsert vti to load data.

    return;
  }

  // helper function for getting and setting a connection for initial
  // setup of the database
  private Connection getConnection() throws SQLException
  {
    if (conn != null)
    {
      if (!conn.isClosed())
        return conn;
      conn = null;
    }
    return conn = makeConnection(true);
  }

  private void closeConnection() throws SQLException
  {
    if (conn != null)
    {
      if (!conn.isClosed())
      {
        try
        {
          conn.commit();
        } finally
        {
          conn.close();
        }
      }
    }
    conn = null;
  }

  // helper functions to run sql schema and database creation scripts
  private void runScript(String scriptBase) throws Exception
  {

    String script = "org/apache/derbyTesting/system/oe/schema/" + scriptBase;
    int errorCount = runScript(script, "US-ASCII");
    assert errorCount == 0;
  }

  public int runScript(String resource, String encoding) throws Exception
  {

    URL sql = getTestResource(resource);

    assert sql != null;

    InputStream sqlIn = openTestResource(sql);
    Connection conn = getConnection();
    int numErrors = runScript(sqlIn, encoding);
    sqlIn.close();

    if (!conn.isClosed() && !conn.getAutoCommit())
      conn.commit();

    return numErrors;
  }

  public int runScript(InputStream script, String encoding) throws Exception
  {
    // Sink output.
    OutputStream sink = new OutputStream() {

      public void write(byte[] b, int off, int len)
      {
      }

      public void write(int b)
      {
      }
    };

    // Use the same encoding as the input for the output.
    return ij.runScript(getConnection(), script, encoding, sink, encoding);
  }

  // helper function for getting database setup and creation scripts 
  private static URL getTestResource(final String name)
  {

    return (URL) AccessController.doPrivileged(new java.security.PrivilegedAction() {

          public Object run()
          {
            return TPCC.class.getClassLoader().getResource(name);

          }

        });
  }

  private static InputStream openTestResource(final URL url) throws Exception
  {
    return (InputStream) AccessController
        .doPrivileged(new java.security.PrivilegedExceptionAction() {

          public Object run() throws IOException
          {
            return url.openStream();

          }

        });
  }

  // construct a database connection
  private Connection makeConnection(boolean create) throws SQLException
  {
    Properties prop = properties;
    if (create)
    {
      prop = (Properties) properties.clone();
      // add create properties to the set of properties
      prop.setProperty("create", "true");
    }

    // seem to need to set this early and in the system properties
    prop.setProperty(PROP_DEADLOCK_TIMEOUT, deadlockTimeout);
    prop.setProperty(PROP_TRANSACTION_TIMEOUT, transactionTimeout);

    return driver.connect(URL_BASE + getDatabaseName(), prop);
  }

  // database name helper functions
  private String getDatabaseName()
  {
    return database.getAbsolutePath();
  }

  // helper function for recursively deleting the database directory
  private boolean deleteDatabase()
  {
    return deleteDirectory(database);
  }

  private static boolean deleteDirectory(File path)
  {
    if (path.exists())
    {
      File[] files = path.listFiles();
      for (int i = 0; i < files.length; i++)
      {
        if (files[i].isDirectory())
        {
          deleteDirectory(files[i]);
        } else
        {
          files[i].delete();
        }
      }
    }
    return path.delete();
  }

  // helper function for interpreting the configuration data
  private void configure()
  {
    String[] args = preprocessArgs(size);

    int totalTx = this.numberOfTerminals * this.transactionsPerTerminal;
    for (int i = 0; i < args.length; i++)
    {
      if ("-numberOfTerminals".equalsIgnoreCase(args[i]))
      {
        this.numberOfTerminals = Integer.parseInt(args[++i]);
      } else if ("-totalTransactions".equalsIgnoreCase(args[i]))
      {
        totalTx = Integer.parseInt(args[++i]);
      } else if ("-scale".equalsIgnoreCase(args[i]))
      {
        this.scale = Short.parseShort(args[++i]);
      } else if ("-numberOfWarehouses".equalsIgnoreCase(args[i]))
      {
        this.numberOfWarehouses = Short.parseShort(args[++i]);
      } else if ("-deadlockTimeout".equalsIgnoreCase(args[i]))
      {
        this.deadlockTimeout = args[++i];
      } else if ("-transactionTimeout".equalsIgnoreCase(args[i]))
      {
        this.transactionTimeout = args[++i];
      } else if ("-retryLimit".equalsIgnoreCase(args[i]))
      {
        this.retryLimit = Integer.parseInt(args[++i]);
      }
    }
    // calculate the transactions per terminals now that we know the 
    // total number to be executed and the number of terminals
    this.transactionsPerTerminal = totalTx / this.numberOfTerminals;

    System.out.println("number of terminals  = " + this.numberOfTerminals);
    System.out.println("total transactions   = "
        + (this.transactionsPerTerminal * this.numberOfTerminals));
    System.out.println("scale                = " + this.scale);
    System.out.println("number of warehouses = " + this.numberOfWarehouses);
    System.out.println("deadlock timeout     = " + this.deadlockTimeout);
    System.out.println("transaction timeout  = " + this.transactionTimeout);
    System.out.println("retry limit          = " + this.retryLimit);
  }

  /*************************************************************************************
   * These methods should really live on the Config class
   */

  // Determine the multi-threading level of this benchmark size.
  // TODO refactor Config
  private int getThreadCount(String size)
  {
    switch (config.getThreadModel())
    {
    case SINGLE:
      return 1;
    case FIXED:
      return config.getThreadFactor(size);
    case PER_CPU: {
      int factor = config.getThreadFactor(size);
      int cpuCount = Runtime.getRuntime().availableProcessors();
      return factor * cpuCount;
    }
    default:
      throw new RuntimeException("Unknown thread model");
    }
  }

  // Retrieve the benchmark arguments for the given size, applying preprocessing
  // as appropriate. The preprocessing that is currently done is:
  //    ${THREADS} - replaced with the specified thread count for the benchmark size
  private String[] preprocessArgs(String size)
  {
    String[] raw = config.getArgs(size);
    String[] cooked = new String[raw.length];
    for (int i = 0; i < raw.length; i++)
    {
      String tmp = raw[i];
      tmp = tmp.replace("${THREADS}", Integer.toString(getThreadCount(size)));
      cooked[i] = tmp;
    }
    return cooked;
  }

}
