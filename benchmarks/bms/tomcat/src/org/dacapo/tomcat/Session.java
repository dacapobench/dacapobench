package org.dacapo.tomcat;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

/**
 * Per-session data
 */
public class Session {

  /**
   * The underlying HttpClient object
   */
  protected final HttpClient httpClient = new HttpClient();

  public static Session create() {
    Session result = new Session();
    result.init();
    return result;
  }
  
  protected Session() {
  }

  /**
   * Allow post-constructor initialization.  This method allows us to do
   * Session session = new Session().init();
   * @return
   */
  protected void init() {
    setConnectionManagerParams(httpClient.getHttpConnectionManager().getParams());
    setClientParams(httpClient.getParams());
    setClientState(httpClient.getState());
  }
  
  /**
   * Set connenction manager parameters.  Subclasses 
   * @param params
   */
  protected void setConnectionManagerParams(HttpConnectionManagerParams params) {
    params.setConnectionTimeout(30000);
  }

  protected void setClientParams(HttpClientParams params) {
  }

  protected void setClientState(HttpState state) {
  }


}
