package org.dacapo.h2o;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RestUtil {

    private String url;
    private String response;

    public RestUtil(String url) {
        this.url = url;
    }

    /**
     * 
     * @param api
     * @return the response data
     * @throws IOException
     */
    public void getMethod(String api) throws IOException {
        URL restURL = new URL(this.url + File.separator + api);

        // Open the connection
        HttpURLConnection conn = (HttpURLConnection) restURL.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200)
            throw new IOException("The returned code is: " + conn.getResponseCode());

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        response = getStringFromBufferedReader(br);

        br.close();
    }

    /**
     * 
     * @param api
     * @param query
     * @return The response data
     * @throws IOException
     */
    public void postMethod(String api, String query) throws IOException {
        URL restURL = new URL(this.url + File.separator + api);

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
            throw new IOException("The returned code is: " + conn.getResponseCode());

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
    protected static String getStringFromBufferedReader(BufferedReader br) {
        StringBuilder lines = new StringBuilder();
        try {
            String line;
            while ((line = br.readLine()) != null)
                lines.append(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines.toString();
    }

    /**
     * Variable @response is supposed to be a JSON format string.
     * given a list of labels, the returning could be the value of this labels or null 
     *  
     * @param labels 
     * @return a key-value map
     */
    public Map<String, String> getFromResponse(String... labels) {
        Map<String, String> labelValue = new HashMap<>();
        JSONObject obj = JSONObject.fromObject(response);
        for (String label : labels){
            // Todo: validate the labels
            // the json responsed from h2o is well-design. The details is in 
            // http://docs.h2o.ai/h2o/latest-stable/h2o-docs/rest-api-reference.html, which contains all RESTFul API and data structure
            labelValue.put(label, obj.getString(label));
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
    Map<String, List> getListFromResponse(String... labels) {
        Map<String, List> labelValue = new HashMap<>();
        JSONObject obj = JSONObject.fromObject(response);
        for (String label : labels){
            // Todo: validate the labels
            // the json responsed from h2o is well-design. The details is in
            // http://docs.h2o.ai/h2o/latest-stable/h2o-docs/rest-api-reference.html, which contains all RESTFul API and data structure
            labelValue.put(label, (List) obj.get(label));
        }
        return labelValue;
    }

    static List<JSONObject> getIndexFromJsonArray(String jl) {
        JSONArray jsonA = JSONArray.fromObject(jl);
        List<JSONObject> l = new LinkedList<>();
        for (int i = 0; i < jsonA.size(); i++){
            l.add(JSONObject.fromObject(jsonA.getString(i)));
        }
        return l;
    }



}
