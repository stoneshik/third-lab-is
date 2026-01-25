package lab.is.services.insertion.coordinator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lab.is.config.BatchProperties;
import lab.is.exceptions.CsvParserException;
import lab.is.exceptions.MinioException;
import lab.is.exceptions.RetryInsertException;
import lab.is.services.insertion.bloomfilter.BloomFilterManager;
import lab.is.services.insertion.history.InsertionHistoryService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportTransactionCoordinatorWrapper {
    private static final Logger logger = LoggerFactory.getLogger(ImportTransactionCoordinatorWrapper.class);
    private final BatchProperties properties;
    private final ImportTransactionCoordinator coordinator;
    private final InsertionHistoryService insertionHistoryService;
    private final BloomFilterManager bloomFilterManager;

    public void execute(
        MultipartFile file,
        long insertionHistoryId
    ) {
        try {
            coordinator.execute(file, insertionHistoryId);
        } catch (MinioException e) {
            rollback(insertionHistoryId, 0L);
            throw e;
        } catch (RetryInsertException e) {
            rollback(insertionHistoryId, e.getRecordCount());
            throw new CsvParserException(
                e.getMessage(),
                e.getRecordCount()
            );
        } catch (CsvParserException e) {
            rollback(insertionHistoryId, e.getRecordCount());
            throw e;
        } catch (Exception e) {
            rollback(insertionHistoryId, 0L);
            throw new CsvParserException(
                "Ошибка при импорте данных",
                0L
            );
        }
    }

    public void rollback(
        Long insertionHistoryId,
        Long recordCount
    ) {
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
