package com.empleosvm.empleovm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

        /**
         * Expone la carpeta /uploads del servidor como rutas accesibles desde el
         * navegador:
         * /uploads/fotos/** → uploads/fotos/
         * /uploads/cvs/** → uploads/cvs/
         *
         * Esto hace que las imágenes y CVs se puedan pedir desde el frontend
         * con rutas como: /uploads/fotos/1234567_foto.jpg
         */
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {

                String uploadsAbsoluto = Paths.get("uploads").toAbsolutePath().toUri().toString();

                registry
                                .addResourceHandler("/uploads/**")
                                .addResourceLocations(uploadsAbsoluto);
        }
}