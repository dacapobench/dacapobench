package org.dacapo.h2o;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class ClientRunner{

    public static void main(String[] args) {
        running("http://127.0.0.1:54321", "/Users/chenrui/Projects/IdeaProjects/Work_ANU/dacapobench/benchmarks/scratch/dat"
                , "slice_localization_data.csv", "drf" , "reference", System.out);
    }

    public static void running(String URL, String data, String source, String algo, String response, PrintStream stdout) {
        DacapoH2OFacade dhf = new DacapoH2OFacade(URL);
        // Import and parse the data set
        stdout.println("Importing file: " + source + "......");
        dhf.importFiles(data + File.separator + source);

        //Paring the data
        dhf.parse(data + File.separator + source);
        //Wait for paring to finish before build a model with the data frame
        stdout.println("Parsing file: " + source + "......");
        dhf.waitForAllJobsToFinish();

        // After parsing, it should be hex files
        source = source.replace("csv", "hex");
        stdout.println("Building model......");

        //Train a model with the parsed data
        dhf.train_drf(source, response);
        //Wait for completion
        dhf.waitForAllJobsToFinish();
        stdout.println("Model built successfully");

        // Delete all frames
        dhf.deleteFrames(source);
        stdout.println("Frames deleted");
    }
}
