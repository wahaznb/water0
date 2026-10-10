package com.water0.hydration.data.fitness;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.health.connect.client.permission.HealthPermissionsRequestAppContract;
import java.util.Set;

/**
 * Interop shim: HealthPermissionsRequestAppContract is public bytecode
 * but Kotlin-internal in connect-client 1.1.0 (the ...Legacy() factory
 * is gone entirely), so Kotlin callers can't name the type. Java can.
 * One static constructor, no logic — lets the provider-app permission
 * sheet stay as a fallback on ROMs without the platform Health module.
 */
public final class HcAppContract {
    private HcAppContract() {
    }

    public static ActivityResultContract<Set<? extends String>, Set<? extends String>> create(
            String providerPackage) {
        return new HealthPermissionsRequestAppContract(providerPackage);
    }
}
