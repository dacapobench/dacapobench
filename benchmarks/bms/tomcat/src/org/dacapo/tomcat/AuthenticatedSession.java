package org.dacapo.tomcat;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpClientParams;

/**
 * An authenticated Tomcat session
 *
 */
public class AuthenticatedSession extends Session {

  private final String username;
  private final String password;
  /**
   * Factory method - forces correct initialization
   * @param username Username
   * @param password Password
   * @param port TCP port
   * @return The created session
   */
  public static Session create(String username, String password, int port) {
    return new AuthenticatedSession(username, password, port).init();
  }

  protected AuthenticatedSession(String username, String password, int port) {
    super(port);
    this.username = username;
    this.password = password;
  }

  @Override
  protected void setClientState(HttpState state) {
    super.setClientState(state);
    Credentials defaultcreds = new UsernamePasswordCredentials(username, password);
    state.setCredentials(new AuthScope("localhost", port, AuthScope.ANY_REALM), defaultcreds);
  }

  @Override
  protected void setClientParams(HttpClientParams params) {
    super.setClientParams(params);
    params.setAuthenticationPreemptive(true);
  }


}
