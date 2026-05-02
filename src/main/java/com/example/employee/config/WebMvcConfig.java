package com.example.employee.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.resume-dir:uploads/applicant-resume}")
    private String resumeUploadDir;

    @Value("${app.upload.overtime-dir:uploads/overtime-attachments}")
    private String overtimeUploadDir;

    @Value("${app.upload.official-business-dir:uploads/official-business-attachments}")
    private String obUploadDir;

    @Value("${app.upload.employee-photo-dir:uploads/employee-photos}")
    private String employeePhotoDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path base = Path.of(resumeUploadDir).toAbsolutePath().normalize();
        String location = base.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/applicant-resume/**")
                .addResourceLocations(location);

        Path otBase = Path.of(overtimeUploadDir).toAbsolutePath().normalize();
        String otLoc = otBase.toUri().toString();
        if (!otLoc.endsWith("/")) {
            otLoc += "/";
        }
        registry.addResourceHandler("/uploads/overtime-attachments/**")
                .addResourceLocations(otLoc);

        Path obBase = Path.of(obUploadDir).toAbsolutePath().normalize();
        String obLoc = obBase.toUri().toString();
        if (!obLoc.endsWith("/")) {
            obLoc += "/";
        }
        registry.addResourceHandler("/uploads/official-business-attachments/**")
                .addResourceLocations(obLoc);

        Path phBase = Path.of(employeePhotoDir).toAbsolutePath().normalize();
        String phLoc = phBase.toUri().toString();
        if (!phLoc.endsWith("/")) {
            phLoc += "/";
        }
        registry.addResourceHandler("/uploads/employee-photos/**")
                .addResourceLocations(phLoc);
    }
}
