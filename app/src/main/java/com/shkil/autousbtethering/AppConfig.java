package com.shkil.autousbtethering;

import java.util.concurrent.TimeUnit;

public class AppConfig {

    public static final boolean DEBUG = BuildConfig.DEBUG;

    public static final long POLLING_INTERVAL = TimeUnit.MINUTES.toMillis(3);

}
