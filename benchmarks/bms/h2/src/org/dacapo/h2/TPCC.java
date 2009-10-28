package org.dacapo.h2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.security.AccessController;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.derbyTesting.system.oe.client.Display;
import org.apache.derbyTesting.system.oe.client.Load;
import org.apache.derbyTesting.system.oe.client.MultiThreadSubmitter;
import org.apache.derbyTesting.system.oe.client.Operations;
import org.apache.derbyTesting.system.oe.client.Submitter;
import org.apache.derbyTesting.system.oe.load.ThreadInsert;
import org.apache.derbyTesting.system.oe.util.OERandom;

import org.dacapo.parser.Config;
import org.h2.tools.RunScript;

public class TPCC
{
  public final static int     RETRY_LIMIT                   = 5;

  // h2 driver settings
  private final static String DRIVER_NAME                   = "org.h2.Driver";
  private final static String URL_BASE                      = "jdbc:h2:";
  private final static String DATABASE_NAME                 = "mem:testdb";
  private final static String CREATE_SUFFIX                 = "";

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

  // Basic configurable items
  // number of threads to perform loading of the database
  private int                 loaderThreads                 = 1;
  
  // this loaderThreads seems determininstic and should be
  // to the number of CPU cores
  private short               scale                         = DEF_SCALE;
  private int                 numberOfTerminals             = DEF_NUM_OF_TERMINALS;
  private int                 transactionsPerTerminal       = DEF_TRANSACTIONS_PER_TERMINAL;
  private short               numberOfWarehouses            = DEF_NUM_OF_WAREHOUSES;

  // OLTP runners
  private Connection[]        connections;
  private Submitter[]         submitters;
  private Display[]           displays;
  private OERandom[]          rands;

  private Config              config;
  private File                scratch;
  private String              size;
  private Driver              driver;
  private Properties          properties;
  private Connection          conn;

  // Location of the database, this specifies the directory (folder) on a file
  // system where the database is stored. This application must have be able to 
  // create this directory (folder)
  private String              database;

  private String              createSuffix                  = CREATE_SUFFIX;
  
  // A random seed for initializing the database and the OLTP terminals.
  private final static long   SEED                          = 897523978813691l;
  private final static int    SEED_STEP                     = 100000;

  public static TPCC make(Config config, File scratch) throws Exception
  {
    return new TPCC(config, scratch);
  }

  public TPCC(Config config, File scratch) throws Exception
  {
    this.config  = config;
    this.scratch = scratch;

    // seem to need to set this early and in the system properties
    Class.forName(DRIVER_NAME);
    driver = DriverManager.getDriver(URL_BASE + DATABASE_NAME);
    
    properties = (Properties) System.getProperties().clone();

    // make the database relative to the scratch location
    if (inMemoryDB())
      database = DATABASE_NAME;
    else
      database = new File(scratch, DATABASE_NAME).getAbsolutePath();

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
    connections = new Connection[numberOfTerminals];
    submitters  = new Submitter[numberOfTerminals];
  }

  public void preIteration(String size) throws Exception
  {
    // we can't change size after the initial prepare(size)
    assert this.size.equalsIgnoreCase(size);

    // create schema
    createSchema();

    // load data
    loadData();

    // create constraints (post load, may want to move this pre load)
    createConstraints();

    // hang on to the connection until after the iteration is complete

    // make sure we have the same seeds each run
    for (int i = 0; i < rands.length; i++)
    {
      rands[i] = new OERandom(SEED_STEP * i, SEED + SEED_STEP * i);
    }

    // create a Submitter for each thread, and then pass to
    // org.apache.derbyTesting.system.oe.client.MultiThreadSubmitter.multiRun
    for (int i = 0; i < submitters.length; i++)
    {
      connections[i] = makeConnection(false);
      
      Operations ops = new Operation(connections[i]);

      submitters[i] = new TPCCSubmitter(null, ops, rands[i], numberOfWarehouses);
    }
    
    // clean up any hang-over from previous iterations
    System.gc();
  }

  public void iteration(String size) throws Exception
  {
    // we can't change size after the initial prepare(size)
    assert this.size.equalsIgnoreCase(size);

    MultiThreadSubmitter.multiRun(submitters, displays, transactionsPerTerminal);
    
    System.out.println();
    
    report(System.err);
  }

  public void postIteration(String size) throws Exception
  {
    // we can't change size after the initial prepare(size)
    assert this.size.equalsIgnoreCase(size);

    for (int i = 0; i < submitters.length; i++)
    {
      submitters[i]  = null;
      connections[i].close();
      connections[i] = null;
    }
    
    closeConnection();
  }

  public void cleanup() throws Exception
  {
    deleteDatabase();
  }

  // ----------------------------------------------------------------------------------
  private void report(PrintStream os)
  {
	os.println("BEGIN: transaction count report");
    for (int i = 0; i < submitters.length; i++)
    {
      submitters[i].printReport(os);
    }
    os.println("END: transaction count report");
  }

  private void createSchema() throws Exception
  {
    // create schema
    runScript("schema.sql");
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
    DataSource ds = new TPCCDataSource(driver, database, properties);
    
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
    ResultSet results = RunScript.execute(getConnection(), new InputStreamReader(script));
    
	if (results!=null) results.close();
	
    return 0;
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

    return driver.connect(URL_BASE + getDatabaseName() + (create?createSuffix:""), prop);
  }

  // database name helper functions
  private String getDatabaseName()
  {
    return database;
  }

  // helper function for recursively deleting the database directory
  private boolean deleteDatabase()
  {
	if (inMemoryDB())
	  return true;
	else
      return deleteDirectory(new File(database));
  }

  private boolean inMemoryDB() {
	return DATABASE_NAME.startsWith("mem:") || DATABASE_NAME.startsWith("memory:"); //  || true;
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
    String[] args = config.preprocessArgs(size,scratch);

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
      } else if ("-createSuffix".equalsIgnoreCase(args[i]))
      {
    	this.createSuffix = args[++i];
      }
    }
    // calculate the transactions per terminals now that we know the 
    // total number to be executed and the number of terminals
    this.transactionsPerTerminal = totalTx / this.numberOfTerminals;
  }


}
