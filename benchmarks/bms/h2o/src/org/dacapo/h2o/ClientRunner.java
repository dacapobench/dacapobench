package org.dacapo.h2o;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.File;

public class ClientRunner{

    public static void running(String URL, String data, String source, String algo, String response) {
        DacapoH2OFacade dhf = new DacapoH2OFacade(URL);
        // Import and parse the data set
        System.out.println("Importing file: " + source + "......");
        dhf.importFiles(data + File.separator + source);

        //Paring the data
        dhf.parse(data + File.separator + source);
        //Wait for paring to finish before build a model with the data frame
        dhf.waitForAllJobsToFinish();
        System.out.println("Parsing file: " + source + "......");

        // After parsing, it should be hex files
        source = source.replace("csv", "hex");
        System.out.println("Building model......");

        //Train a model with the parsed data
        dhf.train_drf(source, response);
        //Wait for completion
        dhf.waitForAllJobsToFinish();
        System.out.println("Model built successfully");
    }
}
