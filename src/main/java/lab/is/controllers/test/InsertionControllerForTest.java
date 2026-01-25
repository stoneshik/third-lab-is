package lab.is.controllers.test;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lab.is.services.insertion.coordinator.ImportTransactionCoordinatorWrapper;
import lab.is.services.insertion.history.InsertionHistoryService;
import lombok.RequiredArgsConstructor;

@Profile({"test", "jmeter"})
@RestController
@RequestMapping("/api/v1/insertion")
@RequiredArgsConstructor
public class InsertionControllerForTest {
    private final ImportTransactionCoordinatorWrapper coordinatorWrapper;
    private final InsertionHistoryService insertionHistoryService;

    @PostMapping("/csv")
    public ResponseEntity<Void> importCsv(
        @RequestParam Long userId,
        @RequestParam MultipartFile file
    ) {
        long insertionHistoryId = insertionHistoryService.create(userId);
        coordinatorWrapper.execute(file, insertionHistoryId);
        return ResponseEntity.ok().build();
    }
}
