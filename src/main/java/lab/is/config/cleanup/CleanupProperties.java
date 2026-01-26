package lab.is.config.cleanup;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "cleanup")
public class CleanupProperties {
    private Ttl ttl = new Ttl();
    private String delayString;

    @Data
    public static class Ttl {
        private Duration tmp;
        private Duration pending;
    }
}
