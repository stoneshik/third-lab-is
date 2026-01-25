package lab.is.config.minio;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class MinioConfig {
    private final MinioProperties properties;

    @Bean
    MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(properties.getEndpoint())
            .credentials(
                properties.getAccessKey(),
                properties.getSecretKey()
            )
            .build();
    }
}
