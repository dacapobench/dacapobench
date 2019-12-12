package org.dacapo.harness.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Timer;

/**
 * Various Http helper routines
 */
public class HttpUtils {

    /**
     * Makes a http request to the specified endpoint
     */
    public static void invokeHttpRequest(URL endpointUrl,
                                           String httpMethod,
                                           Map<String, String> headers,
                                           String requestBody, String fileKey) {
        HttpURLConnection connection = createHttpConnection(endpointUrl, httpMethod, headers);
        try {
            if ( requestBody != null ) {
                DataOutputStream wr = new DataOutputStream(
                        connection.getOutputStream());
                wr.writeBytes(requestBody);
                wr.flush();
                wr.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Request failed. " + e.getMessage(), e);
        }
        executeHttpRequest(connection, fileKey);
    }
    
    public static void executeHttpRequest(HttpURLConnection connection, String fileKey) {
        try {
            // Get Response
            InputStream is;
            try {
                is = connection.getInputStream();
            } catch (IOException e) {
                is = connection.getErrorStream();
            }

            String[] sep = fileKey.split("/");

            FileOutputStream fileOutputStream = new FileOutputStream(fileKey);
            BufferedInputStream in = new BufferedInputStream(is);

            byte[] buffer = new byte[1024];
            int len = 0;

            System.out.println("Downloading the " + sep[sep.length-2] + File.separator + sep[sep.length-1] + "...");
            long start = System.currentTimeMillis();
            while ((len = in.read(buffer)) != -1){
                fileOutputStream.write(buffer,0,len);
            }
            in.close();
            System.out.println(System.currentTimeMillis() - start);
            fileOutputStream.close();
        } catch (Exception e) {
            throw new RuntimeException("Request failed. " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    public static HttpURLConnection createHttpConnection(URL endpointUrl,
                                                         String httpMethod,
                                                         Map<String, String> headers) {
        try {
            HttpURLConnection connection = (HttpURLConnection) endpointUrl.openConnection();
            connection.setRequestMethod(httpMethod);
            
            if ( headers != null ) {
                System.out.println("--------- Request headers ---------");
                for ( String headerKey : headers.keySet() ) {
                    System.out.println(headerKey + ": " + headers.get(headerKey));
                    connection.setRequestProperty(headerKey, headers.get(headerKey));
                }
            }

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            return connection;
        } catch (Exception e) {
            throw new RuntimeException("Cannot create connection. " + e.getMessage(), e);
        }
    }
    
    public static String urlEncode(String url, boolean keepPathSlash) {
        String encoded;
        try {
            encoded = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding is not supported.", e);
        }
        if ( keepPathSlash ) {
            encoded = encoded.replace("%2F", "/");
        }
        return encoded;
    }
}
