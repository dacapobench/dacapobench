/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.h2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.derbyTesting.system.oe.client.Display;
import org.apache.derbyTesting.system.oe.client.Load;
import org.apache.derbyTesting.system.oe.client.MultiThreadSubmitter;
import org.apache.derbyTesting.system.oe.client.Operations;
import org.apache.derbyTesting.system.oe.client.Submitter;
import org.apache.derbyTesting.system.oe.direct.Standard;
import org.apache.derbyTesting.system.oe.load.ThreadInsert;
import org.apache.derbyTesting.system.oe.util.OERandom;

import org.dacapo.parser.Config;
import org.h2.tools.RunScript;
import org.h2.tools.Backup;
import org.h2.tools.Restore;
import org.h2.tools.Script;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: TPCC.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class TPCC {
  public final static int RETRY_LIMIT = 5;

  // h2 driver settings
  private final static String DRIVER_NAME = "org.h2.Driver";
  private final static String URL_BASE = "jdbc:h2:";
  private final static String DATABASE_NAME_DISK = "testdb";
  private final static String DATABASE_NAME_MEMORY = "mem:testdb";
  private final static String CREATE_SUFFIX = "";

  private final static String USERNAME = "user";
  private final static String PASSWORD = "password";
  private final static String USER = "derby";
  private final static String PASS = "derby";
  private final static String BACKUP_NAME = "db.zip";
  private final static String DATABASE_DIRECTORY = "db";

  // default configuration for external testing of derby
  // database scale (see TPC-C documentation) number of terminals (clients) that
  // run transactions
  private final static int DEF_NUM_OF_TERMINALS = 2;
  // database scale (see TPC-C documentation)
  private final static short DEF_SCALE = 1;
  // number of transactions each terminal (client) runs
  private final static int DEF_TRANSACTIONS_PER_TERMINAL = 100;

  // Basic configurable items
  // number of threads to perform loading of the database
  private int loaderThreads = 1;

  // this loaderThreads seems determininstic and should be
  // to the number of CPU cores
  private short scale = DEF_SCALE;
  private int totalTransactions = DEF_NUM_OF_TERMINALS * DEF_TRANSACTIONS_PER_TERMINAL;
  private int numberOfTerminals = DEF_NUM_OF_TERMINALS;
  private int[] transactionsPerTerminal = { DEF_TRANSACTIONS_PER_TERMINAL, DEF_TRANSACTIONS_PER_TERMINAL }; // DEF_TRANSACTIONS_PER_TERMINAL;
  private boolean generate = false; // by default we use the pre-generated
  // database
  private boolean inMemoryDB = true; // by default use the in memory db
  private boolean cleanupInIteration = false; // by default perform clean up in
  // preiteration phase
  private boolean reportPreIterationTimes = false;

  // OLTP runners
  private Connection[] connections;
  private Submitter[] submitters;
  private TPCCReporter reporter = new TPCCReporter();
  private OERandom[] rands;

  private Config config;
  private File scratch;
  private boolean verbose;
  private boolean preserve;
  private String size;
  private Driver driver;
  private Properties properties;
  private Connection conn;

  private boolean firstIteration;

  private long preIterationTime = 0;
  private long resetToInitialDataTime = 0;

  // Location of the database, this specifies the directory (folder) on a file
  // system where the database is stored. This application must have be able to
  // create this directory (folder)
  private String database;

  private String createSuffix = CREATE_SUFFIX;

  // A random seed for initializing the database and the OLTP terminals.
  final static long SEED = 897523978813691l;
  final static int SEED_STEP = 100000;

  public static TPCC make(Config config, File scratch, Boolean verbose, Boolean preserve) throws Exception {
    return new TPCC(config, scratch, verbose, preserve);
  }

  public TPCC(Config config, File scratch, boolean verbose, boolean preserve) throws Exception {
    this.config = config;
    this.scratch = scratch;
    this.verbose = verbose;
    this.preserve = preserve;
  }

  public void prepare(String size) throws Exception {
    this.firstIteration = true;
    this.size = size;

    configure();

    // make the database relative to the scratch location
    if (inMemoryDB)
      database = DATABASE_NAME_MEMORY;
    else
      database = new File(new File(scratch, "db"), DATABASE_NAME_DISK).toURI().toString();

    // seem to need to set this early and in the system properties
    Class.forName(DRIVER_NAME);
    driver = DriverManager.getDriver(URL_BASE + database);

    properties = (Properties) System.getProperties().clone();

    properties.setProperty(USERNAME, USER);
    properties.setProperty(PASSWORD, PASS);

    // make a seeded random number generator for each submitter
    rands = new OERandom[numberOfTerminals];

    // create a set of Submitter each with a Standard operations implementation
    connections = new Connection[numberOfTerminals];
    submitters = new Submitter[numberOfTerminals];
    transactionsPerTerminal = new int[numberOfTerminals];

    // set up the transactions for each terminal
    final int iterationsPerClient = totalTransactions / numberOfTerminals;
    final int oddIterations = totalTransactions - (iterationsPerClient * numberOfTerminals);

    for (int i = 0; i < numberOfTerminals; i++)
      transactionsPerTerminal[i] = iterationsPerClient + (i < oddIterations ? 1 : 0);
  }

  private void preIterationDiskDB() throws Exception {
    File dbDir = new File(scratch, DATABASE_DIRECTORY);

    // delete the database if it exists
    if (preserve && !firstIteration)
      deleteDatabase();

    if (firstIteration && this.generate) {

      // create the database
      createSchema();

      // generate data
      loadData();

      // generate primary indexes
      createIndexes();

      // generate foreign keys
      createConstraints();

      // close last connection returning database to a stable state
      closeConnection();

      // backup the database

      org.h2.tools.Backup.execute(new File(scratch, BACKUP_NAME).getAbsolutePath(), dbDir.getAbsolutePath(), DATABASE_NAME_DISK, true);
    } else {
      org.h2.tools.Restore.execute(new File(scratch, BACKUP_NAME).getAbsolutePath(), dbDir.getAbsolutePath(), DATABASE_NAME_DISK, true);
    }
  }

  private long checkSum = 0;

  private void preIterationMemoryDB() throws Exception {
    if (firstIteration) {
      // create the database
      if (verbose)
        System.out.println("Creating Schema");
      createSchema();

      if (verbose)
        System.out.println("Generating Data");
      // generate the data
      loadData();

      if (verbose)
        System.out.println("Generate Indexes");
      // generate indexes
      createIndexes();

      if (verbose)
        System.out.println("Generate Foreign Key constraints");
      // generate foreign keys
      createConstraints();

      getConnection().commit();

      if (verbose)
        System.out.println("Calculate checksum of initial data");
      checkSum = calculateSumDB();
    } else if (!cleanupInIteration) {
      resetToInitialData();

      long value = calculateSumDB();
      if (value != checkSum)
        System.err.println("Checksum Failed for Database, expected " + checkSum + " got " + value);
    }

    // keep connection open so that database stays in memory
  }

  public void preIteration(String size) throws Exception {
    // we can't change size after the initial prepare(size)
    assert this.size.equalsIgnoreCase(size);

    reporter.reset();

    long start = System.currentTimeMillis();

    TPCCSubmitter.setSeed(SEED);

    if (inMemoryDB)
      preIterationMemoryDB();
    else
      preIterationDiskDB();

    // make sure we have the same seeds each run
    OERandom generator = new OERandom(0, SEED);
    for (int i = 0; i < rands.length; i++) {
      rands[i] = new OERandom(SEED_STEP * i, SEED + SEED_STEP * i);
      // rands[i] = generator;
    }

    // create a Submitter for each thread, and then pass to
    // org.apache.derbyTesting.system.oe.client.MultiThreadSubmitter.multiRun
    for (int i = 0; i < submitters.length; i++) {
      connections[i] = makeConnection(false);

      Operations ops = new Standard(connections[i]);

      submitters[i] = new TPCCSubmitter(reporter, ops, rands[i], scale);
    }

    preIterationTime = System.currentTimeMillis() - start;
  }

  public void iteration(String size) throws Exception {
    // we can't change size after the initial prepare(size)
    assert this.size.equalsIgnoreCase(size);

    // run all the submitters. this is taken from
    // org.apache.derbyTesting.system.oe.client.MultiThreadSubmitter
    Thread[] threads = new Thread[submitters.length];
    for (int i = 0; i < submitters.length; i++) {
      submitters[i].clearTransactionCount();
      threads[i] = newThread(i, submitters[i], transactionsPerTerminal[i]);
    }

    for (int i = 0; i < threads.length; i++)
      threads[i].start();

    // and then wait for them to finish
    for (int i = 0; i < threads.length; i++) {
      try {
        threads[i].join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    // done running the submitters

    System.out.println();

    report(System.out);

    if (inMemoryDB && cleanupInIteration) {
      long start = System.currentTimeMillis();

      resetToInitialData();

      long value = calculateSumDB();
      if (value != checkSum)
        System.err.println("Checksum Failed for Database, expected " + checkSum + " got " + value);

      resetToInitialDataTime = System.currentTimeMillis() - start;
    }
  }

  public void postIteration(String size) throws Exception {
    if (verbose && (firstIteration || !inMemoryDB)) {
      System.out.println("Time to perform pre-iteration phase: " + preIterationTime + " msec");
    }
    if (verbose && (inMemoryDB && cleanupInIteration)) {
      System.out.println("Time to reset data to initial state: " + resetToInitialDataTime + " msec");
    }

    firstIteration = false;

    // we can't change size after the initial prepare(size)
    assert this.size.equalsIgnoreCase(size);

    for (int i = 0; i < submitters.length; i++) {
      submitters[i] = null;
      connections[i].close();
      connections[i] = null;
    }

    if (!preserve && !inMemoryDB)
      deleteDatabase();
  }

  public void cleanup() throws Exception {
    if (!preserve && !inMemoryDB)
      deleteDatabase();
  }

  // ----------------------------------------------------------------------------------
  private void report(PrintStream os) {
    int total = 0;
    int[] transactions = new int[Submitter.NEW_ORDER_ROLLBACK + 1];

    for (int i = 0; i < submitters.length; i++) {
      int[] subTx = submitters[i].getTransactionCount();

      for (int j = 0; j < subTx.length; j++) {
        transactions[j] += subTx[j];
        total += subTx[j];
      }
    }
    System.out.println("Completed " + total + " transactions");
    String dots = "........................";
    for (int i = 0; i < transactions.length; i++) {
      System.out.format("\t" + TPCCSubmitter.TX_NAME[i] + " " + dots.substring(TPCCSubmitter.TX_NAME[i].length()) + "%6d (%4.1f%%)%n", transactions[i],
          100 * ((float) transactions[i] / total));
    }
  }

  private void createSchema() throws Exception {
    // create schema
    runScript("schema.sql");
    runScript("delivery.sql");
  }

  private void createIndexes() throws Exception {
    runScript("primarykey.sql");
    runScript("index.sql");
  }

  private void createConstraints() throws Exception {
    // create key constraints
    runScript("foreignkey.sql");
  }

  private void loadData() throws Exception {

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

  private void reportQuantities() throws Exception {
    reportQuantity("ITEM");
    reportQuantity("DISTRICT");
    reportQuantity("CUSTOMER");
    reportQuantity("ORDERS");
    reportQuantity("ORDERLINE");
    reportQuantity("NEWORDERS");
    reportQuantity("WAREHOUSE");
    reportQuantity("STOCK");
    reportQuantity("HISTORY");
    reportQuantity("DELIVERY_REQUEST");
    reportQuantity("DELIVERY_ORDERS");
  }

  private void reportQuantity(String table) throws Exception {
    PreparedStatement ps;
    ResultSet rs;

    ps = prepareStatement("SELECT COUNT(*) FROM " + table);
    ps.execute();
    rs = ps.getResultSet();
    rs.first();
    System.err.println(table + " #" + rs.getLong(1));
  }

  private long calculateSumDB() throws Exception {
    long result = 0;

    result += calculateSumDB("CUSTOMER", 22);
    result += calculateSumDB("DISTRICT", 11);
    result += calculateSumDB("WAREHOUSE", 9);
    result += calculateSumDB("STOCK", 18);
    result += calculateSumDB("ORDERS", 10);
    result += calculateSumDB("ORDERLINE", 12);
    result += calculateSumDB("NEWORDERS", 5);
    result += calculateSumDB("HISTORY", 9);
    result += calculateSumDB("ITEM", 5);
    result += calculateSumDB("DELIVERY_ORDERS", 6);
    result += calculateSumDB("DELIVERY_REQUEST", 3);

    return result;
  }

  private long calculateSumDB(String table, int columns) throws Exception {
    long result = 0;
    PreparedStatement ps = prepareStatement("SELECT * FROM " + table);

    ps.execute();
    ResultSet rs = ps.getResultSet();

    while (rs.next()) {
      for (int c = 1; c <= columns; c++) {
        String v = rs.getString(c);
        for (int i = 0; v != null && i < v.length(); i++)
          result += v.charAt(i);
      }
    }

    return result;
  }

  private void resetToInitialData() throws Exception {
    System.out.println("Resetting database to initial state");

    // there are no initial delivery requests or orders so remove all
    // residual entries
    prepareStatement("DELETE FROM DELIVERY_REQUEST").execute();
    prepareStatement("DELETE FROM DELIVERY_ORDERS").execute();

    // remove all entries that are not marked as part of the initial\\
    // set of entries
    prepareStatement("DELETE FROM HISTORY WHERE H_INITIAL = FALSE").execute();
    prepareStatement("DELETE FROM NEWORDERS WHERE NO_INITIAL = FALSE").execute();
    prepareStatement("DELETE FROM ORDERLINE WHERE OL_INITIAL = FALSE").execute();
    prepareStatement("DELETE FROM ORDERS WHERE O_INITIAL = FALSE").execute();

    // commit deletes
    getConnection().commit();

    // although below seems a little inefficient we put the conditions in for
    // the
    // following reason: it keeps the commit set size low and therefore there is
    // less heap pressure
    // we also perform regular commits for the same reason
    prepareStatement(
        "UPDATE CUSTOMER SET C_DATA = C_DATA_INITIAL, C_BALANCE = -10.0, C_YTD_PAYMENT = 10.0, C_PAYMENT_CNT = 1, C_DELIVERY_CNT = 0 WHERE C_DATA <> C_DATA_INITIAL OR C_BALANCE <> -10.0 OR C_YTD_PAYMENT <> 10.0 OR C_PAYMENT_CNT <> 1 OR C_DELIVERY_CNT <> 0").execute();
    getConnection().commit();

    prepareStatement("UPDATE DISTRICT SET D_YTD = 30000.0, D_NEXT_O_ID = 3001 WHERE D_YTD <> 30000.0 OR D_NEXT_O_ID <> 3001").execute();
    getConnection().commit();

    prepareStatement("UPDATE WAREHOUSE SET W_YTD = 300000.0 WHERE W_YTD <> 300000.0").execute();
    getConnection().commit();

    prepareStatement("UPDATE STOCK SET S_QUANTITY = S_QUANTITY_INITIAL, S_ORDER_CNT = 0, S_YTD = 0, S_REMOTE_CNT = 0 WHERE S_QUANTITY <> S_QUANTITY_INITIAL OR S_ORDER_CNT <> 0 OR S_YTD <> 0 OR S_REMOTE_CNT <> 0").execute();
    getConnection().commit();

    prepareStatement("UPDATE ORDERS SET O_CARRIER_ID = O_CARRIER_ID_INITIAL WHERE O_CARRIER_ID <> O_CARRIER_ID_INITIAL").execute();
    getConnection().commit();

    prepareStatement("UPDATE ORDERLINE SET OL_DELIVERY_D = OL_DELIVERY_D_INITIAL WHERE OL_DELIVERY_D <> OL_DELIVERY_D_INITIAL").execute();
    getConnection().commit();
  }

  // helper function for getting and setting a connection for initial
  // setup of the database
  private Connection getConnection() throws SQLException {
    if (conn != null) {
      if (!conn.isClosed())
        return conn;
      conn = null;
    }
    return conn = makeConnection(true);
  }

  private void closeConnection() throws SQLException {
    if (conn != null) {
      if (!conn.isClosed()) {
        try {
          conn.commit();
        } finally {
          conn.close();
        }
      }
    }
    conn = null;
  }

  // helper functions to run sql schema and database creation scripts
  private void runScript(String scriptBase) throws Exception {

    String script = "org/apache/derbyTesting/system/oe/schema/" + scriptBase;
    int errorCount = runScript(script, "US-ASCII");
    assert errorCount == 0;
  }

  public int runScript(String resource, String encoding) throws Exception {

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

  public int runScript(InputStream script, String encoding) throws Exception {
    ResultSet results = RunScript.execute(getConnection(), new InputStreamReader(script));

    if (results != null)
      results.close();

    return 0;
  }

  // helper function for getting database setup and creation scripts
  private static URL getTestResource(final String name) {

    return (URL) AccessController.doPrivileged(new java.security.PrivilegedAction() {

      public Object run() {
        return TPCC.class.getClassLoader().getResource(name);

      }

    });
  }

  private static InputStream openTestResource(final URL url) throws Exception {
    return (InputStream) AccessController.doPrivileged(new java.security.PrivilegedExceptionAction() {

      public Object run() throws IOException {
        return url.openStream();

      }

    });
  }

  // construct a database connection
  private Connection makeConnection(boolean create) throws SQLException {
    Properties prop = properties;
    if (create) {
      prop = (Properties) properties.clone();
      // add create properties to the set of properties
      prop.setProperty("create", "true");
    }

    // return driver.connect(URL_BASE + getDatabaseName() +
    // (create?createSuffix:""), prop);
    return driver.connect(getDatabaseURLString(create), prop); // URL_BASE +
    // getDatabaseName()
    // +
    // (create?createSuffix:""),
    // prop);
  }

  private PreparedStatement prepareStatement(String sql) throws SQLException {
    // Prepare all statements as forward-only, read-only, close at commit.
    return getConnection().prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
  }

  // database name helper functions
  private String getDatabaseName() {
    return database;
  }

  // form the database url string
  private String getDatabaseURLString(boolean create) {
    return URL_BASE + getDatabaseName() + (create ? createSuffix : "");
  }

  // helper function for recursively deleting the database directory
  private boolean deleteDatabase() throws SQLException, URISyntaxException {
    if (inMemoryDB) {
      closeConnection();
      return true;
    } else
      return deleteDirectory(new File(new URI(database)));
  }

  private static boolean deleteDirectory(File path) {
    if (path.exists()) {
      File[] files = path.listFiles();
      for (int i = 0; i < files.length; i++) {
        if (files[i].isDirectory()) {
          deleteDirectory(files[i]);
        } else {
          files[i].delete();
        }
      }
    }
    return path.delete();
  }

  // helper function for interpreting the configuration data
  private void configure() {
    String[] args = config.preprocessArgs(size, scratch);

    this.numberOfTerminals = config.getThreadCount(size);

    for (int i = 0; i < args.length; i++) {
      if ("--numberOfTerminals".equalsIgnoreCase(args[i])) {
        this.numberOfTerminals = Integer.parseInt(args[++i]);
      } else if ("--total-transactions".equalsIgnoreCase(args[i])) {
        this.totalTransactions = Integer.parseInt(args[++i]);
      } else if ("--scale".equalsIgnoreCase(args[i])) {
        this.scale = Short.parseShort(args[++i]);
      } else if ("--generate".equalsIgnoreCase(args[i])) {
        this.generate = true;
      } else if ("--memory".equalsIgnoreCase(args[i])) {
        this.inMemoryDB = true;
      } else if ("--disk".equalsIgnoreCase(args[i])) {
        this.inMemoryDB = false;
      } else if ("--report-pre-iteration-times".equalsIgnoreCase(args[i])) {
        this.reportPreIterationTimes = true;
      } else if ("--cleanup-in-iteration".equalsIgnoreCase(args[i])) {
        this.cleanupInIteration = true;
      } else if ("--create-suffix".equalsIgnoreCase(args[i])) {
        this.createSuffix = args[++i];
      }
    }
  }

  private File getBackupDir() {
    return new File(scratch, "backup");
  }

  private File getBackupFile(String fileName) {
    return new File(getBackupDir(), fileName);
  }

  private void backupData() throws Exception {
    getBackupDir().mkdirs();

    runScript(new java.io.StringBufferInputStream("CALL CSVWRITE('" + getBackupFile("orderline.csv").getAbsolutePath() + "', 'SELECT * FROM ORDERLINE', 'UTF-8');"), "US-ASCII");
    runScript(new java.io.StringBufferInputStream("CALL CSVWRITE('" + getBackupFile("neworders.csv").getAbsolutePath() + "', 'SELECT * FROM NEWORDERS', 'UTF-8');"), "US-ASCII");
    runScript(
        new java.io.StringBufferInputStream("CALL CSVWRITE('" + getBackupFile("history.csv").getAbsolutePath() + "', 'SELECT * FROM HISTORY', 'UTF-8');"), "US-ASCII");
    runScript(new java.io.StringBufferInputStream("CALL CSVWRITE('" + getBackupFile("orders.csv").getAbsolutePath() + "', 'SELECT * FROM ORDERS', 'UTF-8');"), "US-ASCII");
    runScript(new java.io.StringBufferInputStream("CALL CSVWRITE('" + getBackupFile("customer.csv").getAbsolutePath() + "', 'SELECT * FROM CUSTOMER', 'UTF-8');"), "US-ASCII");
    runScript(new java.io.StringBufferInputStream("CALL CSVWRITE('" + getBackupFile("stock.csv").getAbsolutePath() + "', 'SELECT * FROM STOCK', 'UTF-8');"), "US-ASCII");
    runScript(new java.io.StringBufferInputStream("CALL CSVWRITE('" + getBackupFile("district.csv").getAbsolutePath() + "', 'SELECT * FROM DISTRICT', 'UTF-8');"), "US-ASCII");
    runScript(new java.io.StringBufferInputStream("CALL CSVWRITE('" + getBackupFile("item.csv").getAbsolutePath() + "', 'SELECT * FROM ITEM', 'UTF-8');"), "US-ASCII");
    runScript(new java.io.StringBufferInputStream("CALL CSVWRITE('" + getBackupFile("warehouse.csv").getAbsolutePath() + "', 'SELECT * FROM WAREHOUSE', 'UTF-8');"), "US-ASCII");
  }

  private void restoreData() throws Exception {
    runScript(new java.io.StringBufferInputStream("INSERT INTO WAREHOUSE SELECT * FROM CSVREAD('" + getBackupFile("warehouse.csv").getAbsolutePath() + "');"), "US-ASCII");
    runScript(new java.io.StringBufferInputStream("INSERT INTO ITEM SELECT * FROM CSVREAD('" + getBackupFile("item.csv").getAbsolutePath() + "');"), "US-ASCII");
    runScript(new java.io.StringBufferInputStream("INSERT INTO DISTRICT SELECT * FROM CSVREAD('" + getBackupFile("district.csv").getAbsolutePath() + "');"), "US-ASCII");
    runScript(new java.io.StringBufferInputStream("INSERT INTO STOCK SELECT * FROM CSVREAD('" + getBackupFile("stock.csv").getAbsolutePath() + "');"), "US-ASCII");
    runScript(new java.io.StringBufferInputStream("INSERT INTO CUSTOMER SELECT * FROM CSVREAD('" + getBackupFile("customer.csv").getAbsolutePath() + "');"), "US-ASCII");
    runScript(new java.io.StringBufferInputStream("INSERT INTO ORDERS SELECT * FROM CSVREAD('" + getBackupFile("orders.csv").getAbsolutePath() + "');"), "US-ASCII");
    runScript(new java.io.StringBufferInputStream("INSERT INTO HISTORY SELECT * FROM CSVREAD('" + getBackupFile("history.csv").getAbsolutePath() + "');"), "US-ASCII");
    runScript(new java.io.StringBufferInputStream("INSERT INTO NEWORDERS SELECT * FROM CSVREAD('" + getBackupFile("neworders.csv").getAbsolutePath() + "');"), "US-ASCII");
    runScript(new java.io.StringBufferInputStream("INSERT INTO ORDERLINE SELECT * FROM CSVREAD('" + getBackupFile("orderline.csv").getAbsolutePath() + "');"), "US-ASCII");
  }

  private static Thread newThread(final int threadId, final Submitter submitter, final int count) {
    Thread t = new Thread("OE_Thread:" + threadId) {

      public void run() {
        try {
          submitter.runTransactions(null, count);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };

    return t;
  }

}
