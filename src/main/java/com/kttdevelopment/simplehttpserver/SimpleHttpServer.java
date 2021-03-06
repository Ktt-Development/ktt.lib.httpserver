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

import com.sun.net.httpserver.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * <i>This class is a simplified implementation of {@link HttpServer}.</i><br>
 * At least one {@link HttpHandler} must be created in order to process requests. When handling requests the server will use the most specific context. If no handler can be found it is rejected with a 404 response. <br>
 * <b>Contexts are case-sensitive.</b>
 *
 * @see HttpServer
 * @see HttpHandler
 * @see SimpleHttpsServer
 * @see SimpleHttpHandler
 * @since 02.00.00
 * @version 03.05.08
 * @author Ktt Development
 */
@SuppressWarnings("SpellCheckingInspection")
public abstract class SimpleHttpServer {

    /**
     * Creates an empty {@link SimpleHttpServer}. Server is created using {@link #create()}, {@link #create(int)}, or {@link #create(int, int)}.
     *
     * @see #create()
     * @see #create(int)
     * @see #create(int, int)
     * @see SimpleHttpServerImpl#createHttpServer(Integer, Integer)
     * @since 02.00.00
     * @author Ktt Development
     */
    protected SimpleHttpServer(){ }

//

    /**
     * Creates a {@link SimpleHttpServer}.
     *
     * @return a {@link SimpleHttpServer}
     * @throws IOException uncaught exception
     *
     * @since 02.00.00
     * @author Ktt Development
     */
    public static SimpleHttpServer create() throws IOException {
        return SimpleHttpServerImpl.createHttpServer(null, null);
    }

    /**
     * Creates a {@link SimpleHttpServer} bounded to a port.
     *
     * @param port port to bind to
     * @return a {@link SimpleHttpServer}
     * @throws java.net.BindException if server can not bind to port
     * @throws NullPointerException if address is <code>null</code>
     * @throws IllegalArgumentException if port is out of range
     * @throws IOException uncaught exception
     *
     * @since 02.00.00
     * @author Ktt Development
     */
    public static SimpleHttpServer create(final int port) throws IOException {
        return SimpleHttpServerImpl.createHttpServer(port, null);
    }

    /**
     * Creates a {@link SimpleHttpServer} bounded to a port.
     *
     * @param port port to bind to
     * @param backlog maximum amount of inbound connections allowed
     * @return a {@link SimpleHttpServer}
     * @throws java.net.BindException if server can not bind to port
     * @throws NullPointerException if address is <code>null</code>
     * @throws IllegalArgumentException if port is out of range
     * @throws IOException uncaught exception
     *
     * @since 02.00.00
     * @author Ktt Development
     */
    public static SimpleHttpServer create(final int port, final int backlog) throws IOException {
        return SimpleHttpServerImpl.createHttpServer(port, backlog);
    }

//

    /**
     * Returns the native http server.
     *
     * @return http server
     *
     * @see HttpServer
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract HttpServer getHttpServer();

//

    /**
     * Binds the server to a port.
     *
     * @param port port to bind the server to
     * @return address the server is binded to
     * @throws java.net.BindException if server could not be bound to port, or if it's already bound
     * @throws IllegalArgumentException if port is out of range
     * @throws NullPointerException if address is <code>null</code>
     * @throws IOException uncaught exception
     *
     * @see #bind(int, int)
     * @see #bind(InetSocketAddress)
     * @see #bind(InetSocketAddress, int)
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract InetSocketAddress bind(final int port) throws IOException;

    /**
     * Binds the server to a port.
     *
     * @param port port to bind the server to
     * @param backlog maximum amount of inbound connections allowed
     * @return address the server is binded to
     * @throws java.net.BindException if server could not be bound to port, or if it's already bound
     * @throws IllegalArgumentException if port is out of range
     * @throws NullPointerException if address is <code>null</code>
     * @throws IOException uncaught exception
     *
     * @see #bind(int)
     * @see #bind(InetSocketAddress)
     * @see #bind(InetSocketAddress, int)
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract InetSocketAddress bind(final int port, final int backlog) throws IOException;

    /**
     * Binds the server to a port.
     *
     * @param addr address to bind the server to
     * @throws java.net.BindException if server could not be bound to port, or if it's already bound
     * @throws IllegalArgumentException if port is out of range
     * @throws NullPointerException if address is <code>null</code>
     * @throws IOException uncaught exception
     *
     * @see InetSocketAddress
     * @see #bind(int)
     * @see #bind(int, int)
     * @see #bind(InetSocketAddress, int)
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract void bind(final InetSocketAddress addr) throws IOException;

    /**
     * Binds the server to a port.
     *
     * @param addr address to bind the server to
     * @param backlog maximum amount of inbound connections allowed
     * @throws java.net.BindException if server could not be bound to port, or if it's already bound
     * @throws NullPointerException if address is <code>null</code>
     * @throws IOException uncaught exception
     *
     * @see InetSocketAddress
     * @see #bind(int)
     * @see #bind(int, int)
     * @see #bind(InetSocketAddress)
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract void bind(final InetSocketAddress addr, final int backlog) throws IOException;

//

    /**
     * Returns the address that the server is binded to.
     *
     * @return binded address
     * @throws NullPointerException if server is not binded to a port
     *
     * @see InetSocketAddress
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract InetSocketAddress getAddress();

//

    /**
     * Sets the server's executor. <br>
     * For unlimited simultaneous threading use {@link java.util.concurrent.Executors#newCachedThreadPool()}; for limited simultaneous threading use {@link java.util.concurrent.Executors#newFixedThreadPool(int)}. <br>
     *
     * @param executor server executor
     *
     * @see #getExecutor()
     * @see Executor
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract void setExecutor(final Executor executor);

    /**
     * Returns the server's executor or null if none exists.
     *
     * @return server executor
     *
     * @see #setExecutor(Executor)
     * @see Executor
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract Executor getExecutor();

//

    /**
     * Sets a session handler for the server.
     *
     * @param sessionHandler session handler
     *
     * @see HttpSessionHandler
     * @see #getHttpSessionHandler()
     * @since 03.03.00
     * @author Ktt Development
     */
    public abstract void setHttpSessionHandler(final HttpSessionHandler sessionHandler);

    /**
     * Returns the session handler for the server or null if none exists.
     *
     * @return server session handler
     *
     * @see HttpSessionHandler
     * @see #setHttpSessionHandler(HttpSessionHandler)
     * @since 03.03.00
     * @author Ktt Development
     */
    public abstract HttpSessionHandler getHttpSessionHandler();

    /**
     * Returns the session associated with an exchange or null if no session handler exists.
     *
     * @param exchange http exchange
     * @return http session
     *
     * @see HttpSession
     * @see #getHttpSession(SimpleHttpExchange)
     * @since 03.03.00
     * @author Ktt Development
     */
    public abstract HttpSession getHttpSession(final HttpExchange exchange);

    /**
     * Returns the session associated with an exchange or null it no session handler exists.
     *
     * @param exchange http exchange
     * @return http session
     *
     * @see HttpSession
     * @see #getHttpSession(HttpExchange)
     * @since 03.03.00
     * @author Ktt Development
     */
    public abstract HttpSession getHttpSession(final SimpleHttpExchange exchange);

//

    /**
     * Creates an empty context.
     * <br>
     * * {@link HttpServer} often does not throw an exception for duplicate contexts. <a href="https://github.com/Ktt-Development/simplehttpserver/issues/86">#86</a>
     *
     * @param context the context
     * @return the http context associated with the context
     * @throws IllegalArgumentException if the context is invalid or taken*
     * @throws NullPointerException if the context is null
     *
     *
     * @see HttpContext
     * @see #createContext(String, HttpHandler)
     * @see #removeContext(String)
     * @see #removeContext(HttpContext)
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract HttpContext createContext(final String context);

    /**
     * Creates a context mapped to a specified {@link HttpHandler}.
     * <br>
     * * {@link HttpServer} often does not throw an exception for duplicate contexts. <a href="https://github.com/Ktt-Development/simplehttpserver/issues/86">#86</a>
     *
     * @param context the context
     * @param handler the handler
     * @return the http context associated with the context
     * @throws IllegalArgumentException if the context is invalid or taken*
     * @throws NullPointerException if the context is null
     *
     * @see HttpContext
     * @see HttpHandler
     * @see #createContext(String)
     * @see #removeContext(String)
     * @see #removeContext(HttpContext)
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract HttpContext createContext(final String context, final HttpHandler handler);

    //

    /**
     * Creates a context mapped to a specific {@link HttpContext} with an {@link Authenticator}.
     * <br>
     * * {@link HttpServer} often does not throw an exception for duplicate contexts. <a href="https://github.com/Ktt-Development/simplehttpserver/issues/86">#86</a>
     *
     * @param context the context
     * @param authenticator authenticator
     * @return the http context associated with the context
     * @throws IllegalArgumentException if the context is invalid or taken*
     * @throws NullPointerException if the context is null
     *
     * @see HttpContext
     * @see Authenticator
     * @see #createContext(String, HttpHandler, Authenticator)
     * @see #removeContext(String)
     * @see #removeContext(HttpContext)
     * @since 03.03.00
     * @author Ktt Development
     */
    public abstract HttpContext createContext(final String context, final Authenticator authenticator);

    /**
     * Creates a context mapped to a specific {@link HttpContext} with an {@link Authenticator}.
     * <br>
     * * {@link HttpServer} often does not throw an exception for duplicate contexts. <a href="https://github.com/Ktt-Development/simplehttpserver/issues/86">#86</a>
     *
     * @param context the context
     * @param handler the handler
     * @param authenticator authenticator
     * @return the http context associated with the context
     * @throws IllegalArgumentException if the context is invalid or taken*
     * @throws NullPointerException if the context is null
     *
     * @see HttpContext
     * @see HttpHandler
     * @see Authenticator
     * @see #createContext(String, Authenticator)
     * @see #removeContext(String)
     * @see #removeContext(HttpContext)
     * @since 03.03.00
     * @author Ktt Development
     */
    public abstract HttpContext createContext(final String context, final HttpHandler handler, final Authenticator authenticator);

//

    /**
     * Removes the context at the context.
     *
     * @param context the context to remove
     * @throws IllegalArgumentException if no handler at that context exists
     * @throws NullPointerException if the context is null
     *
     * @see #removeContext(HttpContext)
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract void removeContext(final String context);

    /**
     * Removes the context from the server.
     * @param context the context to remove
     * @throws IllegalArgumentException if no handler at that context exists
     * @throws NullPointerException if the context is null
     *
     * @see #removeContext(String)
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract void removeContext(final HttpContext context);

    /**
     * Returns the handler mapped to a context or null if none is found.
     *
     * @param context context to retrieve
     * @return handler associated with that context
     *
     * @see #getContextHandler(HttpContext)
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract HttpHandler getContextHandler(final String context);

    /**
     * Returns the handler mapped to a context or null if it is not found.
     *
     * @param context context to retrieve
     * @return handler associated with that context
     *
     * @see #getContextHandler(String)
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract HttpHandler getContextHandler(final HttpContext context);

    /**
     * Returns a copy of the server's contexts and their respective handlers.
     *
     * @return server's contexts
     *
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract Map<HttpContext,HttpHandler> getContexts();

    //

    /**
     * Generates a random context at the root that can be used on the server.
     *
     * @return random safe context
     *
     * @see #getRandomContext(String)
     * @see com.kttdevelopment.simplehttpserver.handler.TemporaryHandler
     * @since 03.03.00
     * @author Ktt Development
     */
    public abstract String getRandomContext();

    /**
     * Generates a random context that can be used on the server.
     *
     * @param context where to generate the random context
     * @return random safe context
     *
     * @see #getRandomContext()
     * @see com.kttdevelopment.simplehttpserver.handler.TemporaryHandler
     * @since 03.03.00
     * @author Ktt Development
     */
    public abstract String getRandomContext(final String context);

//

    /**
     * Starts the server.
     *
     * @throws IllegalStateException if server is not bound to a valid port
     *
     * @see #stop()
     * @see #stop(int)
     * @since 2.00.00
     * @author Ktt Development
     */
    public abstract void start();

    /**
     * Stops the server and all active requests.
     *
     * @see #start()
     * @see #stop(int)
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract void stop();

    /**
     * Stops the server with a delay for remaining requests.
     *
     * @param delay delay until server stops all active requests
     *
     * @see #start()
     * @see #stop()
     * @since 02.00.00
     * @author Ktt Development
     */
    public abstract void stop(final int delay);

}
