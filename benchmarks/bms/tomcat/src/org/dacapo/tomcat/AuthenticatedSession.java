package org.dacapo.tomcat;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpClientParams;

public class AuthenticatedSession extends Session {

  private final String username;
  private final String password;

  /**
   * Factory method - forces correct initialization
   * @param username
   * @param password
   * @return
   */
  public static AuthenticatedSession create(String username, String password) {
    AuthenticatedSession result = new AuthenticatedSession(username, password);
    result.init();
    return result;
  }
  
  protected AuthenticatedSession(String username, String password) {
    this.username = username;
    this.password = password;
  }
  
  @Override
  protected void setClientState(HttpState state) {
    super.setClientState(state);
    Credentials defaultcreds = new UsernamePasswordCredentials(username, password);
    state.setCredentials(new AuthScope("localhost", Control.port, AuthScope.ANY_REALM), defaultcreds);
  }

  @Override
  protected void setClientParams(HttpClientParams params) {
    super.setClientParams(params);
    params.setAuthenticationPreemptive(true);
  }


}
