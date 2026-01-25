package lab.is.controllers.test;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lab.is.dto.responses.insertion.history.WrapperListInsertionHistoriesResponseDto;
import lab.is.services.insertion.history.DownloadFile;
import lab.is.services.insertion.history.InsertionHistoryService;
import lombok.RequiredArgsConstructor;

@Profile({"test", "jmeter"})
@RestController
@RequestMapping("/api/v1/insertion/histories")
@RequiredArgsConstructor
public class InsertionHistoryControllerForTest {
    private final InsertionHistoryService insertionHistoryService;

    @GetMapping
    public ResponseEntity<WrapperListInsertionHistoriesResponseDto> getAll(
        @RequestParam(required = false) Long userId,
        @PageableDefault(page = 0, size = 10, sort = "creationDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (userId == null) {
            return ResponseEntity.ok(
                insertionHistoryService.findAll(pageable)
            );
        }
        return ResponseEntity.ok(
            insertionHistoryService.findAllByUserId(pageable, userId)
        );
    }

    @GetMapping("/{insertionHistoryId}/file")
    public ResponseEntity<Resource> downloadWrapper(
        @RequestParam(required = false) Long userId,
        @PathVariable Long insertionHistoryId
    ) {
        return download(insertionHistoryId, null);
    }

    public ResponseEntity<Resource> download(Long insertionHistoryId, Long userId) {
        DownloadFile downloadFile = insertionHistoryService.download(insertionHistoryId, userId);
        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + downloadFile.getName() + "\""
            )
            .body(new InputStreamResource(downloadFile.getInputStream()));
    }
}
