package org.yuemi.mmoaccessories.api;

import org.jetbrains.annotations.Nullable;

public final class AccessoriesApiProvider {

    @Nullable
    private static volatile AccessoriesApi instance;

    private AccessoriesApiProvider() {}

    @Nullable
    public static AccessoriesApi getApi() {
        return instance;
    }

    public static void setApi(AccessoriesApi api) {
        instance = api;
    }

    public static boolean isAvailable() {
        return instance != null;
    }
}
