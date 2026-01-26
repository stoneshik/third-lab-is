package lab.is.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import lab.is.bd.entities.InsertionHistory;

@Repository
public interface InsertionHistoryRepository extends JpaRepository<InsertionHistory, Long> {
    @Query("SELECT ih FROM InsertionHistory ih WHERE ih.user.id = :userId")
    Page<InsertionHistory> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT ih.fileObjectKey FROM InsertionHistory ih WHERE ih.fileObjectKey is not null")
    Set<String> findAllUsedFileObjectKeys();

    @Query("SELECT ih.fileObjectKey FROM InsertionHistory ih WHERE ih.fileObjectKey LIKE 'tmp/%'")
    Set<String> findAllTmpFileObjectKeys();

    @Query("SELECT ih FROM InsertionHistory ih WHERE ih.status = 'PENDING' AND ih.creationDate < :threshold")
    List<InsertionHistory> findExpiredPending(LocalDateTime threshold);

    @Query("SELECT ih FROM InsertionHistory ih WHERE ih.status = 'SUCCESS' AND ih.fileCommitted = true")
    List<InsertionHistory> findSuccessWithCommittedFile();
}
