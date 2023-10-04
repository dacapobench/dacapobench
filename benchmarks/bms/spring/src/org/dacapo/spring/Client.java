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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

import org.dacapo.harness.LatencyReporter;
import org.dacapo.harness.Digest;


public class Client implements Runnable {

  private static final boolean GEN_DIGESTS = false;  // use this to generate digests when creating a new workload
  private static final String BASE_RQ_URL = "http://localhost:"+Integer.toString(Launcher.SPRING_PORT);

  private static String[] requests;
  private static int cursor = 0;
  private static int fivePercent;
  private static int stride;
  private static int threads;

  private final int ordinal;
  private final LatencyReporter reporter;

  private MultiThreadedHttpConnectionManager connectionManager;
  private HttpClient httpClient;

  public Client(int ordinal, LatencyReporter reporter) {
    this.ordinal = ordinal;
    this.reporter = reporter;
  }

  public static void reset(String[] _requests, int _stride, int _threads) {
    cursor = 0;
    requests = _requests;
    stride = _stride;
    threads = _threads;
    fivePercent = (requests.length/stride)/20;
  }

  private int nextRq(int stride) {
    int rtn;
    synchronized (requests) {
      rtn = cursor;
      cursor += stride;
      int batches = (rtn  / stride);
      if (fivePercent > 0 && batches % fivePercent == 0 && !GEN_DIGESTS) {
        int percentage = 5 * (batches / fivePercent);
        if (percentage <= 100)
          System.out.print("Completing query batches: "+percentage+"%\r");
      }
    }
    return rtn;
  }

  public void run() {
    startSession();
    int rq;
    while ((rq = nextRq(stride)) < requests.length) {
      for (int i = 0; i < stride; i++) {
        String request = requests[rq+i];
        int expectedLength = Integer.parseInt(request.substring(0, 6).trim());
        String expectedDigest = request.substring(7,47);
        String req = request.substring(49);
        switch (request.charAt(48)) {
          case 'G':
            request(new GetMethod(BASE_RQ_URL + req), req, expectedLength, expectedDigest, rq+i);
            break;
          case 'P':
            String[] values = req.split("&");
            HttpMethodBase post = new PostMethod(BASE_RQ_URL + values[0]);
            for(int j = 1; j < values.length; j++) {
              String[] token = values[j].split("=");
              ((PostMethod) post).addParameter(token[0], token[1]);
            }
            request(post, req, expectedLength, expectedDigest, rq+i);
            break;
          default:
            System.err.println("Unexpected request: '"+request+"'");
        }
      }
    }
    shutdownSession();
  }

  static final int DEFAULT_TIMEOUT_MS = 30000;  // 10X more generous than normal default

  private void startSession() {
    connectionManager = new MultiThreadedHttpConnectionManager();
    httpClient = new HttpClient(connectionManager);
    int timeoutms = (int) (DEFAULT_TIMEOUT_MS * Float.parseFloat(System.getProperty("dacapo.timeout.dialation")));
    httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(timeoutms);
  }

  private void shutdownSession() {
    connectionManager.shutdown();
  }

  static final int MAX_MESSAGE_LEN = 208000;
  private void request(HttpMethodBase rq, String request, int expectedLength, String expectedDigest, int index) {
    reporter.start();
    try {
      boolean get = rq instanceof GetMethod;
      final int result = httpClient.executeMethod(rq);
      if (!(result == 200 || (!get && result == 302))) // redirect on post is OK
        System.err.println("Unexpected response. "+Thread. currentThread().getName()+" got "+result+" for request "+index+": "+(get ? "GET " : "POST ")+request);
      else {
        final byte[] buf = rq.getResponseBody(MAX_MESSAGE_LEN);
        if (!GEN_DIGESTS && buf.length != expectedLength)
          System.err.println("Unexpected response length. "+Thread. currentThread().getName()+" got "+buf.length+" but expected "+expectedLength+" for request "+index+": "+(get ? "GET " : "POST ")+request+", got:\n"+(new String(buf, 0, Math.min(buf.length, 1024), StandardCharsets.UTF_8)));
        else {
          String digest = Digest.byteDigest(buf, 2048);
        if (GEN_DIGESTS)
            System.err.println(">>>> "+String.format("%1$6s ", buf.length)+digest+(get ? " G" : " P")+request);
          else if (!digest.equals(expectedDigest))
            System.err.println("Unexpected digest "+Thread. currentThread().getName()+" got "+digest+", but expected "+expectedDigest+" for request "+index+": "+(get ? "GET " : "POST ")+request+", got:\n"+(new String(buf, 0, Math.min(buf.length, 1024), StandardCharsets.UTF_8)));
        }
      }
    } catch (IOException e) {
      System.err.println("IOException: "+e);
      System.exit(-1);
    }
    reporter.end();
  }
}
