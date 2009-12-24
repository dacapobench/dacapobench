/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.h2;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

/**
 * Simple DataSource for providing a wrapped connection for the Derby TPC-C to
 * populate the database.
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: TPCCDataSource.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class TPCCDataSource implements DataSource {

  private final static String USERNAME = "user";
  private final static String PASSWORD = "password";
  private final static String USER = "derby";
  private final static String PASS = "derby";

  private Driver driver;
  private String dbname;
  private Properties properties;
  private PrintWriter logWriter;
  private int loginTimeout = 100;

  public TPCCDataSource(Driver driver, String dbname, Properties properties) {
    this.driver = driver;
    this.dbname = dbname;
    this.properties = properties;
    this.logWriter = new PrintWriter(new NullWriter());
  }

  public Connection getConnection() throws SQLException {
    return driver.connect(dbname, properties);
  }

  public Connection getConnection(String username, String password) throws SQLException {
    Properties props = (Properties) properties.clone();

    props.setProperty(USERNAME, USER);
    props.setProperty(PASSWORD, PASS);

    return driver.connect(dbname, properties);
  }

  public PrintWriter getLogWriter() throws SQLException {
    return logWriter;
  }

  public int getLoginTimeout() throws SQLException {
    return 100;
  }

  public void setLogWriter(PrintWriter logWriter) throws SQLException {
    this.logWriter = logWriter;
  }

  public void setLoginTimeout(int loginTimeout) throws SQLException {
    this.loginTimeout = loginTimeout;
  }

  public boolean isWrapperFor(Class<?> arg0) throws SQLException {
    return false;
  }

  public <T> T unwrap(Class<T> arg0) throws SQLException {
    return null;
  }

  private class NullWriter extends Writer {

    public void close() throws IOException {
    }

    public void flush() throws IOException {
    }

    public void write(char[] arg0, int arg1, int arg2) throws IOException {
    }

  };
}
