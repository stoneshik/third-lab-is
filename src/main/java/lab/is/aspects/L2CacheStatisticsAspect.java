package lab.is.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lab.is.services.CacheStatisticsService;
import lombok.RequiredArgsConstructor;

@Aspect
@Component
@ConditionalOnProperty(
    name = "cache.statistics.enabled",
    havingValue = "true"
)
@RequiredArgsConstructor
public class L2CacheStatisticsAspect {
    private static final Logger logger = LoggerFactory.getLogger(L2CacheStatisticsAspect.class);
    private final CacheStatisticsService statisticsService;

    @Around("execution(* lab.is.repositories..*(..))")
    public Object logCacheStats(ProceedingJoinPoint pjp) throws Throwable {
        Statistics stats = statisticsService.getStatistics();

        long hitsBefore = stats.getSecondLevelCacheHitCount();
        long missesBefore = stats.getSecondLevelCacheMissCount();

        Object result = pjp.proceed();

        long hitsAfter = stats.getSecondLevelCacheHitCount();
        long missesAfter = stats.getSecondLevelCacheMissCount();

        logger.info(
            "L2 Cache stats for {} → hits: +{}, misses: +{}",
            pjp.getSignature().toShortString(),
            hitsAfter - hitsBefore,
            missesAfter - missesBefore
        );

        return result;
    }
}
