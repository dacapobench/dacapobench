package org.dacapo.h2o;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RestUtil {

    private Method fromObjectOb;
    private Method getStringOb;
    private Method getOb;
    private Method fromObjectAr;
    private Method getStringAr;
    private Method getSizeAr;

    private String url;
    private String response;

    RestUtil(String url) throws ClassNotFoundException, NoSuchMethodException {
        this.url = url;
        Class<?> jsonObj = Class.forName("net.sf.json.JSONObject", true, Thread.currentThread().getContextClassLoader());
        fromObjectOb = jsonObj.getMethod("fromObject", Object.class);
        getStringOb = jsonObj.getMethod("getString", String.class);
        getOb = jsonObj.getMethod("get", String.class);

        Class<?> jsonArr = Class.forName("net.sf.json.JSONArray", true, Thread.currentThread().getContextClassLoader());
        fromObjectAr = jsonArr.getMethod("fromObject", Object.class);
        getStringAr = jsonArr.getMethod("getString", int.class);
        getSizeAr = jsonArr.getMethod("size");
    }

    /**
     * 
     * @param api
     * @return the response data
     * @throws IOException
     */
    public void getMethod(String api) throws IOException {
        URL restURL = new URL(this.url + "/" + api.replace("evaluation-git+", "evaluation-git%2B"));

        // Open the connection
        HttpURLConnection conn = (HttpURLConnection) restURL.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200)
            throw new IOException("Request failed for "+restURL+" with error code: " + conn.getResponseCode());

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        response = getStringFromBufferedReader(br);

        br.close();
    }

    public void deleteMethod(String api) throws IOException {
        URL restURL = new URL(this.url + "/" + api.replace("evaluation-git+", "evaluation-git%2B"));

        // Open the connection
        HttpURLConnection conn = (HttpURLConnection) restURL.openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty(
                "Content-Type", "application/x-www-form-urlencoded" );
        conn.setRequestMethod("DELETE");
        conn.connect();
    }

    /**
     * 
     * @param api
     * @param query
     * @return The response data
     * @throws IOException
     */
    public void postMethod(String api, String query) throws IOException {
        URL restURL = new URL(this.url + "/" + api.replace("evaluation-git+", "evaluation-git%2B"));
        query = query.replace("evaluation-git+", "evaluation-git%2B");

        // Open the connection
        HttpURLConnection conn = (HttpURLConnection) restURL.openConnection();
        conn.setConnectTimeout(5*1000); // 5 seconds
        conn.setReadTimeout(5*1000); // 5 seconds
        // Set POST, and open for input and output
        conn.setRequestMethod("POST");
//        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setDoInput(true);

        PrintStream ps = new PrintStream(conn.getOutputStream());
        // Write and Flush
        ps.print(query);
        ps.close();
        
        // testing if there is a positive response 
        if (conn.getResponseCode() != 200)
            throw new IOException("Request failed for "+restURL+" with error code: " + conn.getResponseCode());

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        response = getStringFromBufferedReader(br);

        br.close();
    }

    /**
     * Read the string from BufferedReader several times. And return the whole as a single string 
     * 
     * @param br
     * @return the concatenation of the string from @br
     */
    protected static String getStringFromBufferedReader(BufferedReader br) throws IOException {
        StringBuilder lines = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null)
            lines.append(line);
        return lines.toString();
    }

    /**
     * Variable @response is supposed to be a JSON format string.
     * given a list of labels, the returning could be the value of this labels or null 
     *  
     * @param labels 
     * @return a key-value map
     */
    public Map<String, String> getFromResponse(String... labels) throws InvocationTargetException, IllegalAccessException {
        Map<String, String> labelValue = new HashMap<>();
        Object obj = fromObjectOb.invoke(null, (Object) response);
        for (String label : labels){
            // Todo: validate the labels
            // the json responsed from h2o is well-design. The details is in
            // http://docs.h2o.ai/h2o/latest-stable/h2o-docs/rest-api-reference.html, which contains all RESTFul API and data structure
            labelValue.put(label, (String) getStringOb.invoke(obj, (Object) label));
        }
        return labelValue;
    }

    /**
     * Variable @response is supposed to be a JSON format string.
     * same as getFromResponse,
     *
     * @param labels
     * @return a key-value map
     */
    Map<String, List> getListFromResponse(String... labels) throws InvocationTargetException, IllegalAccessException {
        Map<String, List> labelValue = new HashMap<>();
        Object obj  = fromObjectOb.invoke(null, (Object) response);
        for (String label : labels){
            // Todo: validate the labels
            // the json responsed from h2o is well-design. The details is in
            // http://docs.h2o.ai/h2o/latest-stable/h2o-docs/rest-api-reference.html, which contains all RESTFul API and data structure
            labelValue.put(label, (List) getOb.invoke(obj, (Object) label));
        }
        return labelValue;
    }

    List getIndexFromJsonArray(String jl) throws InvocationTargetException, IllegalAccessException {
        List l = new LinkedList<>();
        Object jsonA = fromObjectAr.invoke(null, (Object) jl);
        for (int i = 0; i < (Integer) getSizeAr.invoke(jsonA); i++){
            l.add(fromObjectOb.invoke(null, getStringAr.invoke(jsonA, (Object) i)));
        }
        return l;
    }



}
