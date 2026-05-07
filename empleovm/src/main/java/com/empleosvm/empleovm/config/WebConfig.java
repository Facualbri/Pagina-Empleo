package com.empleosvm.empleovm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. Obtenemos la ruta absoluta de la carpeta donde están las fotos
        // Usamos "user.dir" para pararnos en la raíz del proyecto (empleovm)
        Path rutaCarpeta = Paths.get(System.getProperty("user.dir"), "uploads", "fotos");
        
        // 2. Convertimos a formato de recurso de Spring (file:///C:/...)
        String rutaFinal = "file:///" + rutaCarpeta.toAbsolutePath().toString().replace("\\", "/") + "/";

        // 3. Mapeo: Cuando el navegador pida "/uploads/fotos/..." 
        // Spring lo va a buscar a la carpeta física.
        registry.addResourceHandler("/uploads/fotos/**")
                .addResourceLocations(rutaFinal)
                .setCachePeriod(0); // 🔥 Evita que el navegador guarde la imagen vieja mientras testeas

        System.out.println("\n---------------------------------------------------------");
        System.out.println("🚀 SERVIDOR DE IMÁGENES ACTIVO");
        System.out.println("📍 Buscando fotos en: " + rutaFinal);
        System.out.println("🌐 Acceso vía: http://localhost:8080/uploads/fotos/1778024280146_SAM_1029.jpg");
        System.out.println("---------------------------------------------------------\n");
    }
}