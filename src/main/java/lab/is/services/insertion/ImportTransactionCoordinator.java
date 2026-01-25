package lab.is.services.insertion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lab.is.config.BatchProperties;
import lab.is.exceptions.CsvParserException;
import lab.is.exceptions.MinioException;
import lab.is.exceptions.RetryInsertException;
import lab.is.services.importfile.MinioImportFileStorage;
import lab.is.services.insertion.bloomfilter.BloomFilterManager;
import lab.is.services.insertion.history.InsertionHistoryService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportTransactionCoordinator {
    private static final Logger logger = LoggerFactory.getLogger(ImportTransactionCoordinator.class);
    private final MinioImportFileStorage fileStorage;
    private final RetryableInsertionService insertionService;
    private final BatchProperties properties;
    private final InsertionHistoryService insertionHistoryService;
    private final BloomFilterManager bloomFilterManager;

    @Transactional
    public void execute(
        MultipartFile file,
        long insertionHistoryId
    ) {
        String objectKey = null;
        try {
            // PHASE 1 — prepare
            objectKey = fileStorage.prepare(
                file.getInputStream(),
                file.getOriginalFilename()
            );
            insertionHistoryService.attachFile(insertionHistoryId, objectKey);
            // PHASE 2 — DB
            Long numberObjects = insertionService.insertWithRetry(
                file.getInputStream()
            );
            // COMMIT
            fileStorage.commit(objectKey);
            insertionHistoryService.markFileCommittedAndStatusToSuccess(
                insertionHistoryId,
                numberObjects
            );
        } catch (MinioException e) {
            rollback(objectKey, insertionHistoryId, 0L);
            throw e;
        } catch (RetryInsertException e) {
            rollback(objectKey, insertionHistoryId, e.getRecordCount());
            throw new CsvParserException(
                e.getMessage(),
                e.getRecordCount()
            );
        } catch (CsvParserException e) {
            rollback(objectKey, insertionHistoryId, e.getRecordCount());
            throw e;
        } catch (Exception e) {
            rollback(objectKey, insertionHistoryId, 0L);
            throw new CsvParserException(
                "Ошибка при импорте данных",
                0L
            );
        }
    }

    public void rollback(
        String objectKey,
        Long insertionHistoryId,
        Long recordCount
    ) {
        if (objectKey != null) {
            fileStorage.rollback(objectKey);
        }
        try {
            insertionHistoryService.updateStatusToFailed(insertionHistoryId);
        } catch (Exception updateHistoryException) {
            logger.warn("не получилось обновить статус истории вставки на failed");
        }
        if (recordCount >= properties.getMaxRecordNumberForRebuildBloomFilter()) {
            bloomFilterManager.rebuild();
        }
    }
}

