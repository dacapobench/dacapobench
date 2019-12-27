package org.dacapo.harness;

import java.io.File;

public class RunWasabiDownload {

    /** Put your access key here **/
    private static final String awsAccessKey = "0YHJF51AI241CMLVPXIA";
    
    /** Put your secret key here **/
    private static final String awsSecretKey = "1uQzF2Rl9CzCb8ygtD4eJJ45zVipHTD4Nbar7c1R";
    
    /** Put your bucket name here **/
    private static final String dataBucketName = "dacapodata";
    private static final String jarBucketName = "dacapo";

    /** The name of the region where the bucket is created. (e.g. us-west-1) **/
    private static final String regionName = "us-west-1";
    
    
    /**
     * Run all the included samples. Before running the samples, you need to
     * specify the bucket name, region name and your credentials.
     */
    public static void Download(String fileKey, String targetDir) {
        DataDownload(fileKey, targetDir);
        JarDownload(fileKey, targetDir);
    }

    /**
     * Down the DaCapo data zip file dependencies into target directory
     * @param fileKey dependencies name
     * @param targetDir target directory
     */
    public static void DataDownload(String fileKey, String targetDir){
        String stringTarget = targetDir+File.separator+"dat";
        CreateIfNotExist(stringTarget);
        GetS3Object.getS3Object(dataBucketName, regionName, awsAccessKey, awsSecretKey, fileKey, stringTarget);
    }

    /**
     * Down the DaCapo jar zip file dependencies into target directory
     * @param fileKey dependencies name
     * @param targetDir target directory
     */

    public static void JarDownload(String fileKey, String targetDir){
        String stringTarget = targetDir+File.separator+"jar";
        CreateIfNotExist(stringTarget);
        GetS3Object.getS3Object(jarBucketName, regionName, awsAccessKey, awsSecretKey, fileKey, stringTarget);
    }

    /**
     * Create target directory if the directory not exist
     * @param directoryTarget a string that represents the path to the directory
     */
    private static void CreateIfNotExist(String directoryTarget) {
        File target = new File(directoryTarget);
        if (!target.exists()) {
            boolean created = target.mkdir();
            if (!created) {
                if (!target.exists()){
                    System.err.println("Failed for creating target directory" + directoryTarget);
                    System.exit(-1);
                }
            }
        }
    }
}
