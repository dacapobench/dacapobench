/*
 * Copyright (c) 2004 - 2007, Tranql project contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.tranql.connector.h2;

import org.h2.jdbcx.JdbcDataSource;
import org.tranql.connector.NoExceptionsAreFatalSorter;
import org.tranql.connector.jdbc.AbstractLocalDataSourceMCF;

/**
 * ManagedConnectionFactory that wraps a H2 DataSource for use in-VM.
 *
 * @version $Revision: 508 $ $Date: 2007-09-21 12:02:45 +1000 (Fri, 21 Sep 2007) $
 */
public class LocalMCF extends AbstractLocalDataSourceMCF {
    private final JdbcDataSource ds;

    /**
     * Default constructor for a H2 Local DataSource.
     */
    public LocalMCF() {
        super(new JdbcDataSource(), new NoExceptionsAreFatalSorter(), false);
        ds = (JdbcDataSource) super.dataSource;
    }

    /**
     * Return the user name used to establish the connection.
     *
     * @return the user name used to establish the connection
     */
    public String getUserName() {
        return ds.getUser();
    }

    /**
     * Set the user name used establish the connection.
     * This value is used if no connection information is supplied by the application
     * when attempting to create a connection.
     *
     * @param user the user name used to establish the connection; may be null
     */
    public void setUserName(String user) {
        ds.setUser(user);
    }

    /**
     * Return the password credential used to establish the connection.
     *
     * @return the password credential used to establish the connection
     */
    public String getPassword() {
        return ds.getPassword();
    }

    /**
     * Set the user password credential establish the connection.
     * This value is used if no connection information is supplied by the application
     * when attempting to create a connection.
     *
     * @param password the password credential used to establish the connection; may be null
     */
    public void setPassword(String password) {
        ds.setPassword(password);
    }

    /**
     * Return the current url for the data source.
     *
     * @return the url. 
     */
    public String getConnectionURL() {
        return ds.getURL();
    }

    /**
     * Set the url for the data source.
     *
     * @param url The url 
     */
    public void setConnectionURL(String url) {
        ds.setURL(url);
    }
}
