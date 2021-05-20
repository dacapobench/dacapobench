package org.dacapo.spring;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.IOException;
// import java.io.File;
import java.net.Socket;
// import java.util.concurrent.CountDownLatch;
// import java.util.concurrent.locks.Condition;
// import java.util.concurrent.locks.ReentrantLock;

import jdk.nashorn.internal.ir.annotations.Ignore;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

import org.dacapo.harness.LatencyReporter;

public class Client implements Runnable {

  private final int ordinal;
  private final LatencyReporter reporter;
  private MultiThreadedHttpConnectionManager connectionManager;
  private HttpClient httpClient;

  private static final int PORT = 8080;

  public Client(int ordinal, LatencyReporter reporter) {
    this.ordinal = ordinal;
    this.reporter = reporter;
  }

  public void run() {
    startSession();

    get("/");
    get("/");
    get("/resources/css/petclinic.css");
    get("/resources/images/favicon.png");
    get("/vets.html");
    get("/owners/find");
    // get("/oups");
    get("/resources/images/pets.png");
    get("/webjars/jquery/2.2.4/jquery.min.js");

    shutdownSession();
  }

  private void startSession() {
    connectionManager = new MultiThreadedHttpConnectionManager();
    httpClient = new HttpClient(connectionManager);
    httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
  }

  private void shutdownSession() {
    connectionManager.shutdown();
  }

  public void get(String address) {
    reporter.start(ordinal);
    try {
        GetMethod method = new GetMethod(formatUrl(PORT, address));

        final int iGetResultCode = httpClient.executeMethod(method);
        final String strGetResponseBody = readStream(method.getResponseBodyAsStream());
        final String strGetResponseBodyLocalized = strGetResponseBody.replace("\n", System.getProperty("line.separator"));

        System.err.println("RC: "+iGetResultCode);
        System.err.println("B: "+address);
        System.err.println(strGetResponseBodyLocalized);
    } catch (IOException e) { 
      System.err.println("IOException: "+e);
    }
    reporter.end(ordinal);
  }


    protected static String readStream(InputStream responseStream) throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(responseStream));
        StringBuilder reply1 = new StringBuilder(4096);
        for (String line = input.readLine(); line != null; line = input.readLine()) {
          reply1.append(line);
          reply1.append('\n');
        }
        input.close();
        StringBuilder reply = reply1;
    
        String replyString = reply.toString();
        return replyString;
    }

    static String formatUrl(int port, String addr) {
        String formattedUrl = String.format("http://localhost:%d%s", port, addr);
        return formattedUrl;
      }

}
