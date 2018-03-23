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

import java.sql.SQLException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.security.auth.Subject;
import javax.sql.XAConnection;

import org.h2.jdbcx.JdbcDataSource;
import org.tranql.connector.CredentialExtractor;
import org.tranql.connector.NoExceptionsAreFatalSorter;
import org.tranql.connector.jdbc.AbstractXADataSourceMCF;
import org.tranql.connector.jdbc.ManagedXAConnection;

/**
 * ManagedConnectionFactory that wraps a H2 XADataSource for use in-VM.
 *
 * @version $Revision: 508 $ $Date: 2007-09-21 12:02:45 +1000 (Fri, 21 Sep 2007) $
 */
public class XAMCF extends AbstractXADataSourceMCF {
    private final JdbcDataSource ds;

    /**
     * Default constructor for a H2 XA DataSource.
     */
    public XAMCF() {
        super(new JdbcDataSource(), new NoExceptionsAreFatalSorter());
        ds = (JdbcDataSource) xaDataSource;
    }

    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        CredentialExtractor credentialExtractor = new CredentialExtractor(subject, connectionRequestInfo, this);

        XAConnection sqlConnection = getPhysicalConnection(subject, credentialExtractor);
        try {
            return new ManagedXAConnection(this, sqlConnection, credentialExtractor, exceptionSorter);
        } catch (SQLException e) {
            throw new ResourceAdapterInternalException("Could not set up ManagedXAConnection", e);
        }
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
