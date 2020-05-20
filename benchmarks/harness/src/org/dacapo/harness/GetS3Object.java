package org.dacapo.harness;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


import org.dacapo.harness.auth.AWS4SignerBase;
import org.dacapo.harness.auth.AWS4SignerForAuthorizationHeader;
import org.dacapo.harness.util.HttpUtils;

/**
 * Samples showing how to GET an object from Amazon S3 using Signature V4
 * authorization.
 */
public class GetS3Object {
    
    /**
     * Request the content of the object '/ExampleObject.txt' from the given
     * bucket in the given region using virtual hosted-style object addressing.
     */
        public static void getS3Object(String bucketName, String regionName, String awsAccessKey, String awsSecretKey, String fileKey, String targetDir) {

        // https://s3.us-west-1.wasabisys.com/dacapodata/fop.zip
        // the region-specific endpoint to the target object expressed in path style
        URL endpointUrl;
        try {
            endpointUrl = new URL("https://s3." + regionName + ".wasabisys.com/" + bucketName + "/" + fileKey);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to parse service endpoint: " + e.getMessage());
        }
        
        // for a simple GET, we have no body so supply the precomputed 'empty' hash
        Map<String, String> headers = new HashMap<String, String>();

        headers.put("x-amz-content-sha256", AWS4SignerBase.EMPTY_BODY_SHA256);
        
        AWS4SignerForAuthorizationHeader signer = new AWS4SignerForAuthorizationHeader(
                endpointUrl, "GET", "s3", regionName);

        String authorization = signer.computeSignature(headers,
                                                       null, // no query parameters
                                                       AWS4SignerBase.EMPTY_BODY_SHA256, 
                                                       awsAccessKey, 
                                                       awsSecretKey);
                
        // place the computed signature into a formatted 'Authorization' header
        // and call S3
        headers.put("Authorization", authorization);
        System.out.println(headers);
        HttpUtils.invokeHttpRequest(endpointUrl, "GET", headers, null, targetDir + File.separator + fileKey);
    }
}
