package ai.soc.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Intercepts every HTTP request, reads the X-Tenant-ID header (or other source),
 * and sets the tenant context for Hibernate multi-tenancy.
 */
@Component
@Order(1) // ensure it runs early in the filter chain
@Slf4j
public class TenantFilter implements Filter {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;

        // Read tenant identifier (e.g. from header)
        String tenantId = httpReq.getHeader(TENANT_HEADER);

        if (tenantId != null && !tenantId.isBlank()) {
            TenantIdentifierResolver.setTenant(tenantId.trim());
            log.info("TenantFilter - Using Tenant: " + tenantId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // Always clear to avoid leaking tenant info across threads
            TenantIdentifierResolver.clear();
        }
    }
}