package lab.is.services;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CacheStatisticsService {
    private final EntityManagerFactory entityManagerFactory;

    public Statistics getStatistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }
}
