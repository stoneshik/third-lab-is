package lab.is.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lab.is.exceptions.UserDoesNotHaveEnoughRightsException;
import lab.is.security.model.UserDetailsImpl;
import lab.is.services.insertion.ImportTransactionCoordinator;
import lab.is.services.insertion.history.InsertionHistoryService;
import lombok.RequiredArgsConstructor;

@Profile({"dev", "helios", "default"})
@RestController
@RequestMapping("/api/v1/insertion")
@RequiredArgsConstructor
public class InsertionController {
    private final ImportTransactionCoordinator coordinator;
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
        coordinator.execute(file, insertionHistoryId);
        return ResponseEntity.ok().build();
    }
}
