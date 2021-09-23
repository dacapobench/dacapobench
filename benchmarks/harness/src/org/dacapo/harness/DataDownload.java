package org.dacapo.harness;

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;

import javax.sound.sampled.SourceDataLine;

public class DataDownload {

    private static final String propertiesFile = "https://github.com/dacapobench/dacapobench/blob/dev-chopin/benchmarks/dacapo.properties";
    private static final String urlRegex = "dacapo.zip.url=(https:.*)\\s";
    
    /**
     * Run all the included samples. Before running the samples, you need to
     * specify the bucket name, region name and your credentials.
     */
    public static void download(String fileKey, String targetDir) {
        download(fileKey, targetDir+File.separator+"dat", "dat");
        download(fileKey, targetDir+File.separator+"jar", "jar");
    }

    /**
     * Down the DaCapo data zip file dependencies into target directory
     * @param fileKey dependencies name
     * @param targetDir target directory
     */
    public static void download(String fileName, String path, String kind) {
        String urlstr = "";
        try {
            File tgtdir = new File(path);
            tgtdir.mkdir();
            String dst = tgtdir.getAbsolutePath()+File.separator+fileName;
         //   String bkt = kind.equals("jar") ? jarBucketName : dataBucketName;
            urlstr = getURLFromPropertiesFile()+"/"+fileName;
            URL url = new URL(urlstr);
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            FileOutputStream fos = new FileOutputStream(dst);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            System.out.println("Downloaded: "+urlstr+" to "+dst);
        } catch (Exception e) {
            System.out.println("Failed to download "+fileName+" from "+urlstr+" with exception: "+e+". Exiting.");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static String extractURLFromStream(InputStream stream, String pattern) throws IOException {
        if (stream != null) {
            Writer strWriter = new StringWriter();
            Pattern r = Pattern.compile(pattern);

            char[] buffer = new char[2048];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                int count;
                while ((count = reader.read(buffer)) != -1) {
                    strWriter.write(buffer, 0, count);
                    Matcher m = r.matcher(new String(buffer));
                    if (m.find()) {
                        return m.group(1);
                    }
                }
            } finally {
                stream.close();
            }
            return null;
        } else {
            return null;
        }
    }
    
    private static String getURLFromPropertiesFile() {
        try {
            URL url = new URL(propertiesFile);
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            Map<String, List<String>> header = http.getHeaderFields();
    
            // Redirect if we get 301 or 302 redirection code
            for (String h : header.get(null)) {
                if (h.contains(" 302 ") || h.contains(" 301 ")) {
                    url = new URL(header.get("Location").get(0));
                    http = (HttpURLConnection) url.openConnection();
                    header = http.getHeaderFields();
                }
            }
            return extractURLFromStream(http.getInputStream(), urlRegex);
        } catch (Exception e) {
            System.out.println("Failed to download properties file: "+propertiesFile);
        }
        return null;
    }
}
