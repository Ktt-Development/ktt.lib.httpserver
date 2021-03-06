/*
 * Copyright (C) 2021 Ktt Development
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.kttdevelopment.simplehttpserver;

import com.sun.net.httpserver.HttpExchange;

import java.util.HashMap;

/**
 * A session keeps track of a single client across multiple exchanges. This is typically used for login persistence.
 *
 * @since 02.00.00
 * @version 03.04.01
 * @author Ktt Development
 */
public abstract class HttpSession {

    static final HashMap<String,HttpSession> sessions = new HashMap<>();

    /**
     * Creates an empty {@link HttpSession}. Sessions are usually created by {@link HttpSessionHandler#getSession(HttpExchange)}.
     *
     * @since 02.00.00
     * @author Ktt Development
     */
    protected HttpSession(){ }

//

    /**
     * Returns the session id.
     *
     * @return session id
     *
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract String getSessionID();

//

    /**
     * Returns when the session was created.
     *
     * @return creation time
     *
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract long getCreationTime();

    /**
     * Returns when the session was last used.
     *
     * @return last access time
     *
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract long getLastAccessTime();

    /**
     * Updates the last access time for the session
     *
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract void updateLastAccessTime();

}
