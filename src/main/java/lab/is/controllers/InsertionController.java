package lab.is.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lab.is.exceptions.CsvParserException;
import lab.is.exceptions.RetryInsertException;
import lab.is.exceptions.UserDoesNotHaveEnoughRightsException;
import lab.is.security.model.UserDetailsImpl;
import lab.is.services.insertion.RetryableInsertionService;
import lab.is.services.insertion.history.InsertionHistoryService;
import lombok.RequiredArgsConstructor;

@Profile({"dev", "helios", "default"})
@RestController
@RequestMapping("/api/v1/insertion")
@RequiredArgsConstructor
public class InsertionController {
    private final RetryableInsertionService insertionService;
    private final InsertionHistoryService insertionHistoryService;

    @PostMapping("/csv")
    public ResponseEntity<Void> importCsv(
        @AuthenticationPrincipal UserDetailsImpl userDetails,
        @RequestParam Long userId,
        @RequestParam MultipartFile file
    ) {
        Long userIdFromUserDetails = userDetails.getId();
        if (!userId.equals(userIdFromUserDetails)) {
            throw new UserDoesNotHaveEnoughRightsException("у пользователя недостаточно прав");
        }
        long insertionHistoryId = insertionHistoryService.create(userId);
        try {
            Long numberObjects = insertionService.insertWithRetry(file.getInputStream(), insertionHistoryId);
            insertionHistoryService.updateStatusToSuccess(insertionHistoryId, numberObjects);
            return ResponseEntity.ok().build();
        } catch (RetryInsertException e) {
            throw new CsvParserException(
                e.getMessage(),
                insertionHistoryId,
                e.getRecordCount()
            );
        } catch (CsvParserException e) {
            throw e;
        } catch (Exception e) {
            throw new CsvParserException(
                "Ошибка при импорте данных",
                insertionHistoryId,
                0L
            );
        }
    }
}
