package lab.is.services.cleanup;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lab.is.bd.entities.InsertionHistory;
import lab.is.config.cleanup.CleanupProperties;
import lab.is.repositories.InsertionHistoryRepository;
import lab.is.services.importfile.MinioImportFileService;
import lab.is.services.insertion.history.InsertionHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileCleanupJob {
    private final CleanupProperties properties;
    private final MinioImportFileService importFileService;
    private final InsertionHistoryRepository insertionHistoryRepository;
    private final InsertionHistoryService insertionHistoryService;

    @Scheduled(fixedDelayString = "#{@cleanupProperties.delayString}")
    @Transactional
    public void cleanupAndFixConsistency() {
        cleanupTmpFiles();
        fixExpiredPending();
        fixBrokenSuccess();
        cleanupOrphanFiles();
    }

    /**
     * 1. Чистка tmp-файлов
     */
    public void cleanupTmpFiles() {
        Duration tmpTtl = properties.getTtl().getTmp();
        Instant threshold = Instant.now().minus(tmpTtl);
        List<String> tmpFiles = importFileService.listObjects("tmp/");
        Set<String> usedKeys = insertionHistoryRepository.findAllTmpFileObjectKeys();
        for (String objectKey : tmpFiles) {
            boolean usedInDb = usedKeys.contains(objectKey);
            Instant createdAt = importFileService.getObjectCreationTime(objectKey);
            if (!usedInDb || createdAt.isBefore(threshold)) {
                importFileService.delete(objectKey);
                log.warn("Deleted orphan tmp file {}", objectKey);
            }
        }
    }

    /**
     * 2. PENDING -> FAILED если завис
     */
    public void fixExpiredPending() {
        Duration pendingTtl = properties.getTtl().getPending();
        LocalDateTime threshold = LocalDateTime.now().minus(pendingTtl);
        List<InsertionHistory> expired = insertionHistoryRepository.findExpiredPending(threshold);
        for (InsertionHistory insertionHistory: expired) {
            insertionHistoryService.updateStatusToFailed(insertionHistory.getId());
            log.warn("InsertionHistory {} moved PENDING -> FAILED (timeout)", insertionHistory.getId());
        }
    }

    /**
     * 3. SUCCESS без файла -> FAILED
     */
    public void fixBrokenSuccess() {
        List<InsertionHistory> histories = insertionHistoryRepository.findSuccessWithCommittedFile();
        for (InsertionHistory insertionHistory: histories) {
            String fileObjectKey = insertionHistory.getFileObjectKey();
            if (fileObjectKey == null || !importFileService.exists(fileObjectKey)) {
                insertionHistoryService.updateStatusToFailed(insertionHistory.getId());
                log.error(
                    "InsertionHistory {} SUCCESS but file missing -> FAILED",
                    insertionHistory.getId()
                );
            }
        }
    }

    /**
     * 4. Удаление committed orphan-файлов
     */
    public void cleanupOrphanFiles() {
        Set<String> usedKeys = insertionHistoryRepository.findAllUsedFileObjectKeys();
        cleanupPrefixOrphanFiles("committed/", usedKeys);
    }

    public void cleanupPrefixOrphanFiles(String prefix, Set<String> usedKeys) {
        List<String> files = importFileService.listObjects(prefix);
        for (String objectKey : files) {
            if (!usedKeys.contains(objectKey)) {
                importFileService.delete(objectKey);
                log.warn("Deleted orphan file {}", objectKey);
            }
        }
    }
}
