package com.empleosvm.empleovm.controllers;

import com.empleosvm.empleovm.repository.EmpleoRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeoController {

    private final EmpleoRepository empleoRepository;

    public SeoController(EmpleoRepository empleoRepository) {
        this.empleoRepository = empleoRepository;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots(HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);
        return "User-agent: *\n"
                + "Allow: /\n"
                + "\n"
                + "Sitemap: " + baseUrl + "/sitemap.xml\n";
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap(HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        xml.append("  <url><loc>").append(baseUrl).append("/</loc><priority>1.0</priority></url>\n");
        xml.append("  <url><loc>").append(baseUrl).append("/login.html</loc><priority>0.5</priority></url>\n");
        xml.append("  <url><loc>").append(baseUrl).append("/registro.html</loc><priority>0.5</priority></url>\n");
        xml.append("  <url><loc>").append(baseUrl).append("/empleos.html</loc><priority>0.9</priority></url>\n");

        var empleos = empleoRepository.findByActivoTrue();
        for (var e : empleos) {
            String titulo = e.getTitulo() != null ? e.getTitulo().replaceAll("&", "&amp;") : "";
            xml.append("  <url>\n")
                .append("    <loc>").append(baseUrl).append("/empleos.html?id=").append(e.getId()).append("</loc>\n")
                .append("    <priority>0.7</priority>\n")
                .append("  </url>\n");
        }

        xml.append("</urlset>\n");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(xml.toString());
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getHeader("Host");
        if (host == null) host = "localhost:8080";
        return scheme + "://" + host;
    }
}
