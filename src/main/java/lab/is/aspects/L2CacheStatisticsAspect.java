package lab.is.aspects;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
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
    public Object logL2CacheStatistics(
        ProceedingJoinPoint proceedingJoinPoint
    ) throws Throwable {
        Statistics stats = statisticsService.getStatistics();
        if (!stats.isStatisticsEnabled()) {
            return proceedingJoinPoint.proceed();
        }
        // ===== BEFORE =====
        long l2HitsBefore    = stats.getSecondLevelCacheHitCount();
        long l2MissesBefore  = stats.getSecondLevelCacheMissCount();
        long l2PutsBefore    = stats.getSecondLevelCachePutCount();

        long qHitsBefore     = stats.getQueryCacheHitCount();
        long qMissesBefore   = stats.getQueryCacheMissCount();
        long qPutsBefore     = stats.getQueryCachePutCount();

        Object result = proceedingJoinPoint.proceed();

        // ===== AFTER =====
        long l2HitsAfter     = stats.getSecondLevelCacheHitCount();
        long l2MissesAfter   = stats.getSecondLevelCacheMissCount();
        long l2PutsAfter     = stats.getSecondLevelCachePutCount();

        long qHitsAfter      = stats.getQueryCacheHitCount();
        long qMissesAfter    = stats.getQueryCacheMissCount();
        long qPutsAfter      = stats.getQueryCachePutCount();

        // ===== DELTAS =====
        long dHits    = l2HitsAfter   - l2HitsBefore;
        long dMisses  = l2MissesAfter - l2MissesBefore;
        long dPuts    = l2PutsAfter   - l2PutsBefore;

        long dQHits   = qHitsAfter    - qHitsBefore;
        long dQMisses = qMissesAfter  - qMissesBefore;
        long dQPuts   = qPutsAfter    - qPutsBefore;

        if (
            dHits != 0 ||
            dMisses != 0 ||
            dPuts != 0 ||
            dQHits != 0 ||
            dQMisses != 0 ||
            dQPuts != 0
        ) {
            MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
            String repositoryInterface = signature.getDeclaringType().getSimpleName();
            logger.info(
                """
                [L2 CACHE ACTIVITY]
                Repository interface : {}
                Target proxy class   : {}
                Method               : {}({})
                Entity cache:
                  hits   : +{}
                  misses : +{}
                  puts   : +{}
                Query cache:
                  hits   : +{}
                  misses : +{}
                  puts   : +{}
                """,
                repositoryInterface,
                signature.getName(),
                Arrays.toString(signature.getParameterTypes()),
                dHits,
                dMisses,
                dPuts,
                dQHits,
                dQMisses,
                dQPuts
            );
        }
        return result;
    }
}
