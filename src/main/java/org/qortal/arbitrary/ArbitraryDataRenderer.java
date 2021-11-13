package org.qortal.arbitrary;

import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.qortal.api.HTMLParser;
import org.qortal.arbitrary.ArbitraryDataFile.*;
import org.qortal.data.transaction.ArbitraryTransactionData;
import org.qortal.data.transaction.ArbitraryTransactionData.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ArbitraryDataRenderer {

    private static final Logger LOGGER = LogManager.getLogger(ArbitraryDataRenderer.class);

    private String resourceId;
    private ResourceIdType resourceIdType;
    private String inPath;
    private String secret58;
    private String prefix;
    private boolean usePrefix;
    private boolean async;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private ServletContext context;

    public ArbitraryDataRenderer(String resourceId, ResourceIdType resourceIdType, String inPath, String secret58,
                                 String prefix, boolean usePrefix, boolean async,
                                 HttpServletRequest request, HttpServletResponse response, ServletContext context) {

        this.resourceId = resourceId;
        this.resourceIdType = resourceIdType;
        this.inPath = inPath;
        this.secret58 = secret58;
        this.prefix = prefix;
        this.usePrefix = usePrefix;
        this.async = async;
        this.request = request;
        this.response = response;
        this.context = context;
    }

    public HttpServletResponse render() {
        if (!inPath.startsWith(File.separator)) {
            inPath = File.separator + inPath;
        }

        ArbitraryTransactionData.Service service = Service.WEBSITE;
        ArbitraryDataReader arbitraryDataReader = new ArbitraryDataReader(resourceId, resourceIdType, service, null);
        arbitraryDataReader.setSecret58(secret58); // Optional, used for loading encrypted file hashes only
        try {
            if (!arbitraryDataReader.isCachedDataAvailable()) {
                // If async is requested, show a loading screen whilst build is in progress
                if (async) {
                    arbitraryDataReader.loadAsynchronously();
                    return this.getLoadingResponse();
                }

                // Otherwise, hang the request until the build completes
                arbitraryDataReader.loadSynchronously(false);
            }

        } catch (Exception e) {
            LOGGER.info(String.format("Unable to load %s %s: %s", service, resourceId, e.getMessage()));
            return this.getResponse(response, 500, "Error 500: Internal Server Error");
        }

        java.nio.file.Path path = arbitraryDataReader.getFilePath();
        if (path == null) {
            return this.getResponse(response, 404, "Error 404: File Not Found");
        }
        String unzippedPath = path.toString();

        try {
            String filename = this.getFilename(unzippedPath.toString(), inPath);
            String filePath = unzippedPath + File.separator + filename;

            if (HTMLParser.isHtmlFile(filename)) {
                // HTML file - needs to be parsed
                byte[] data = Files.readAllBytes(Paths.get(filePath)); // TODO: limit file size that can be read into memory
                HTMLParser htmlParser = new HTMLParser(resourceId, inPath, prefix, usePrefix);
                data = htmlParser.replaceRelativeLinks(filename, data);
                response.setContentType(context.getMimeType(filename));
                response.setContentLength(data.length);
                response.getOutputStream().write(data);
            }
            else {
                // Regular file - can be streamed directly
                File file = new File(filePath);
                FileInputStream inputStream = new FileInputStream(file);
                response.setContentType(context.getMimeType(filename));
                int bytesRead, length = 0;
                byte[] buffer = new byte[10240];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    response.getOutputStream().write(buffer, 0, bytesRead);
                    length += bytesRead;
                }
                response.setContentLength(length);
                inputStream.close();
            }
            return response;
        } catch (FileNotFoundException | NoSuchFileException e) {
            LOGGER.info("Unable to serve file: {}", e.getMessage());
            if (inPath.equals("/")) {
                // Delete the unzipped folder if no index file was found
                try {
                    FileUtils.deleteDirectory(new File(unzippedPath));
                } catch (IOException ioException) {
                    LOGGER.info("Unable to delete directory: {}", unzippedPath, e);
                }
            }
        } catch (IOException e) {
            LOGGER.info("Unable to serve file at path: {}", inPath, e);
        }

        return this.getResponse(response, 404, "Error 404: File Not Found");
    }

    private String getFilename(String directory, String userPath) {
        if (userPath == null || userPath.endsWith("/") || userPath.equals("")) {
            // Locate index file
            List<String> indexFiles = this.indexFiles();
            for (String indexFile : indexFiles) {
                String filePath = directory + File.separator + indexFile;
                if (Files.exists(Paths.get(filePath))) {
                    return userPath + indexFile;
                }
            }
        }
        return userPath;
    }

    private HttpServletResponse getLoadingResponse() {
        String responseString = null;
        URL url = Resources.getResource("loading/index.html");
        try {
            responseString = Resources.toString(url, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.info("Unable to show loading screen: {}", e.getMessage());
        }
        return this.getResponse(response, 503, responseString);
    }

    public static HttpServletResponse getResponse(HttpServletResponse response, int responseCode, String responseString) {
        try {
            byte[] responseData = responseString.getBytes();
            response.setStatus(responseCode);
            response.setContentLength(responseData.length);
            response.getOutputStream().write(responseData);
        } catch (IOException e) {
            LOGGER.info("Error writing {} response", responseCode);
        }
        return response;
    }

    private List<String> indexFiles() {
        List<String> indexFiles = new ArrayList<>();
        indexFiles.add("index.html");
        indexFiles.add("index.htm");
        indexFiles.add("default.html");
        indexFiles.add("default.htm");
        indexFiles.add("home.html");
        indexFiles.add("home.htm");
        return indexFiles;
    }

}
