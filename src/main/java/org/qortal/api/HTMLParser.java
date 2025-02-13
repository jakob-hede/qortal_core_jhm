package org.qortal.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.qortal.arbitrary.misc.Service;

import java.util.Objects;

public class HTMLParser {

    private static final Logger LOGGER = LogManager.getLogger(HTMLParser.class);

    private String linkPrefix;
    private byte[] data;
    private String qdnContext;
    private String resourceId;
    private Service service;
    private String identifier;
    private String path;
    private String theme;

    public HTMLParser(String resourceId, String inPath, String prefix, boolean usePrefix, byte[] data,
                      String qdnContext, Service service, String identifier, String theme) {
        this.linkPrefix = usePrefix ? String.format("%s/%s", prefix, resourceId) : "";
        this.data = data;
        this.qdnContext = qdnContext;
        this.resourceId = resourceId;
        this.service = service;
        this.identifier = identifier;
        this.path = inPath;
        this.theme = theme;
    }

    public void addAdditionalHeaderTags() {
        String fileContents = new String(data);
        Document document = Jsoup.parse(fileContents);
        String baseUrl = this.linkPrefix;
        Elements head = document.getElementsByTag("head");
        if (!head.isEmpty()) {
            // Add q-apps script tag
            String qAppsScriptElement = String.format("<script src=\"/apps/q-apps.js?time=%d\">", System.currentTimeMillis());
            head.get(0).prepend(qAppsScriptElement);

            // Add q-apps gateway script tag if in gateway mode
            if (Objects.equals(this.qdnContext, "gateway")) {
                String qAppsGatewayScriptElement = String.format("<script src=\"/apps/q-apps-gateway.js?time=%d\">", System.currentTimeMillis());
                head.get(0).prepend(qAppsGatewayScriptElement);
            }

            // Escape and add vars
            String service = this.service.toString().replace("\"","\\\"");
            String name = this.resourceId != null ? this.resourceId.replace("\"","\\\"") : "";
            String identifier = this.identifier != null ? this.identifier.replace("\"","\\\"") : "";
            String path = this.path != null ? this.path.replace("\"","\\\"") : "";
            String theme = this.theme != null ? this.theme.replace("\"","\\\"") : "";
            String qdnContextVar = String.format("<script>var _qdnContext=\"%s\"; var _qdnTheme=\"%s\"; var _qdnService=\"%s\"; var _qdnName=\"%s\"; var _qdnIdentifier=\"%s\"; var _qdnPath=\"%s\"; var _qdnBase=\"%s\";</script>", this.qdnContext, theme, service, name, identifier, path, baseUrl);
            head.get(0).prepend(qdnContextVar);

            // Add base href tag
            String baseElement = String.format("<base href=\"%s/\">", baseUrl);
            head.get(0).prepend(baseElement);

            // Add meta charset tag
            String metaCharsetElement = "<meta charset=\"UTF-8\">";
            head.get(0).prepend(metaCharsetElement);

        }
        String html = document.html();
        this.data = html.getBytes();
    }

    public static boolean isHtmlFile(String path) {
        if (path.endsWith(".html") || path.endsWith(".htm")) {
            return true;
        }
        return false;
    }

    public byte[] getData() {
        return this.data;
    }
}
