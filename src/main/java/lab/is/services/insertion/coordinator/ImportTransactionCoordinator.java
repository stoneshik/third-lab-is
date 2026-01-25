package lab.is.services.insertion.coordinator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lab.is.services.importfile.MinioImportFileStorage;
import lab.is.services.insertion.RetryableInsertionService;
import lab.is.services.insertion.history.InsertionHistoryService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportTransactionCoordinator {
    private final MinioImportFileStorage fileStorage;
    private final RetryableInsertionService insertionService;
    private final InsertionHistoryService insertionHistoryService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void execute(
        MultipartFile file,
        long insertionHistoryId
    ) throws Exception {
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
        } catch (Exception e) {
            rollback(objectKey);
            throw e;
        }
    }

    public void rollback(String objectKey) {
        if (objectKey != null) {
            fileStorage.rollback(objectKey);
        }
    }
}

