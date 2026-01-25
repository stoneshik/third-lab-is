package lab.is.services.importfile;

import java.io.InputStream;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lab.is.config.minio.MinioProperties;
import lab.is.exceptions.MinioException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MinioImportFileStorage implements ImportFileStorage {
    private static final Logger logger = LoggerFactory.getLogger(MinioImportFileStorage.class);
    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Override
    public String prepare(InputStream data, String filename) {
        String tempKey = "tmp/" + UUID.randomUUID() + "-" + filename;
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(tempKey)
                    .stream(
                        data,
                        -1,
                        properties.getUpload().getPartSize().toBytes()
                    )
                    .contentType("text/csv")
                    .build()
            );
            return tempKey;
        } catch (Exception e) {
            throw new MinioException("Не получилось загрузить файл на MinIO");
        }
    }

    @Override
    public void commit(String objectKey) {
        String finalKey = objectKey.replace("tmp/", "committed/");
        try {
            minioClient.copyObject(
                CopyObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(finalKey)
                    .source(
                        CopySource.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
                    )
                    .build()
            );
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build()
            );
        } catch (Exception e) {
            throw new MinioException("Не получилось закоммитить в MinIO");
        }
    }

    @Override
    public void rollback(String objectKey) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build()
            );
        } catch (Exception ignored) {
            logger.warn("ошибка во время rollback MinIO");
        }
    }

    public InputStream download(String objectKey) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build()
            );
        } catch (Exception e) {
            throw new MinioException("Не удалось скачать файл");
        }
    }
}
