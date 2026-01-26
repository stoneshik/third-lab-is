package lab.is.services.importfile;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lab.is.config.minio.MinioProperties;
import lab.is.exceptions.MinioException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MinioImportFileService {
    private static final Logger logger = LoggerFactory.getLogger(MinioImportFileService.class);
    private final MinioClient minioClient;
    private final MinioProperties properties;

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

    public List<String> listObjects(String prefix) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(properties.getBucket())
                    .prefix(prefix)
                    .recursive(true)
                    .build()
            );
            List<String> keys = new ArrayList<>();
            for (Result<Item> result : results) {
                keys.add(result.get().objectName());
            }
            return keys;
        } catch (Exception e) {
            throw new MinioException("Не удалось получить список объектов MinIO");
        }
    }

    public Instant getObjectCreationTime(String objectKey) {
        try {
            return minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build()
            ).lastModified().toInstant();
        } catch (Exception e) {
            throw new MinioException("Не удалось получить время создания объекта " + objectKey);
        }
    }

    public boolean exists(String objectKey) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new MinioException("Ошибка проверки существования объекта " + objectKey);
        }
    }

    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .build()
            );
        } catch (Exception e) {
            throw new MinioException("Не удалось удалить объект " + objectKey);
        }
    }
}
