package com.kttdevelopment.simplehttpserver.handler;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * This class caches file bytes when adding to the {@link FileHandler}.
 *
 * @see FileHandlerAdapter
 * @see FileHandler
 * @since 4.0.0
 * @version 4.0.0
 * @author Ktt Development
 */
public class CacheFileAdapter implements FileHandlerAdapter {

    private final long cacheTimeMillis;

    /**
     * Creates a CacheFileAdapter where files will expire after set milliseconds.
     *
     * @param cacheTimeMillis how long a file should exist for
     *
     * @since 4.0.0
     * @author Ktt Development
     */
    public CacheFileAdapter(final long cacheTimeMillis){
        this.cacheTimeMillis = cacheTimeMillis;
    }

    /**
     * Creates a CacheFileAdapter where files will expire after a set time.
     *
     * @param cacheTime how long a file should exist for
     * @param timeUnit the time unit
     *
     * @see TimeUnit
     * @since 4.0.0
     * @author Ktt Development
     */
    public CacheFileAdapter(final long cacheTime, final TimeUnit timeUnit){
        cacheTimeMillis = timeUnit.toMillis(cacheTime);
    }

    /**
     * Returns the cached bytes given a file.
     *
     * @param file file to read
     * @return cached file bytes
     *
     * @see #getBytes(File, byte[])
     * @since 4.0.0
     * @author Ktt Development
     */
    // todo
    final byte[] getBytes(final File file){
        return getBytes(file, new byte[0]);
    }

    // todo: toString

}