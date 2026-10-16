package com.basketballleague;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Activates JAX-RS and mounts every resource under {@code /api}.
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
}
