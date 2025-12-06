package ai.soc.config;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    private static final String DEFAULT_TENANT = "dagger";

    // Each request thread stores its tenant id here
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public static void setTenant(String tenantId) {
        currentTenant.set(tenantId);
    }

    public static void clear() {
        currentTenant.remove();
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = currentTenant.get();
        log.info("TenantIdentifierResolver -> " + tenant);
        return (tenant != null && !tenant.isBlank()) ? tenant : DEFAULT_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        // Return true to allow session reuse if tenant hasn’t changed
        return true;
    }
}