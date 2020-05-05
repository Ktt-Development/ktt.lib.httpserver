package com.kttdevelopment.simplehttpserver;

import com.sun.net.httpserver.HttpExchange;

import java.util.*;

/**
 * This class assigns {@link HttpSession} to every client.
 *
 * @since 03.03.00
 * @version 03.03.00
 * @author Ktt Development
 */
public class HttpSessionHandler {

    private final Map<String,HttpSession> sessions = new HashMap<>();

    private final String cookie;

    /**
     * Creates a session handler using the cookie <code>__session-id</code>.
     *
     * @since 03.03.00
     * @author Ktt Development
     */
    public HttpSessionHandler(){
        cookie = "__session-id";
    }

    /**
     * Creates a session handler, storing the id at the specified cookie.
     *
     * @param cookie cookie to store session id
     *
     * @since 03.03.00
     * @author Ktt Development
     */
    public HttpSessionHandler(final String cookie){
        this.cookie = cookie;
    }

    /**
     * Assigns a session id to a client. It is important to make sure duplicate ids do not occur.
     *
     * @param exchange http exchange
     * @return session id
     *
     * @since 03.03.00
     * @author Ktt Development
     */
    public synchronized String assignSessionID(final HttpExchange exchange){
        String id;
        do id = UUID.randomUUID().toString();
            while(sessions.containsKey(id));
        return id;
    }

    /**
     * Assigns a session to the client. Session will only be saved client side if the exchange headers are sent using {@link HttpExchange#sendResponseHeaders(int, long)}.
     *
     * @param exchange http exchange
     *
     * @since 03.03.00
     * @author Ktt Development
     */
    public synchronized final void assignSession(final HttpExchange exchange){
        final String sessionId;
        final HttpSession session;

        final String rcookies = exchange.getRequestHeaders().getFirst("Cookie");
        final Map<String,String> cookies = new HashMap<>();

        if(rcookies != null && !rcookies.isEmpty()){
            final String[] pairs = rcookies.split("; ");
            for(final String pair : pairs){
                final String[] value = pair.split("=");
                cookies.put(value[0],value[1]);
            }
        }

        if((sessionId = cookies.get(cookie)) == null || !sessions.containsKey(sessionId)){
            session = new HttpSession() {

                private final String sessionID;
                private final long creationTime;
                private long lastAccessTime;

                {
                    sessionID = assignSessionID(exchange);
                    creationTime = System.currentTimeMillis();
                    lastAccessTime = creationTime;
                    sessions.put(sessionID,this);
                }

                @Override
                public final String getSessionID(){
                    return sessionID;
                }

            //

                @Override
                public final long getCreationTime(){
                    return creationTime;
                }

                @Override
                public final long getLastAccessTime(){
                    return lastAccessTime;
                }

                @Override
                public synchronized final void updateLastAccessTime(){
                    lastAccessTime = System.currentTimeMillis();
                }

            //

                @SuppressWarnings("StringBufferReplaceableByString")
                @Override
                public final String toString(){
                    final StringBuilder OUT = new StringBuilder();
                    OUT.append("HttpSession")   .append("{");
                    OUT.append("sessionID")     .append("=")   .append(sessionID)          .append(", ");
                    OUT.append("creationTime")  .append("=")   .append(creationTime)       .append(", ");
                    OUT.append("lastAccessTime").append("=")   .append(lastAccessTime);
                    OUT.append("}");
                    return OUT.toString();
                }

            };

            final SimpleHttpCookie out =
                new SimpleHttpCookie.Builder(cookie,session.getSessionID())
                    .setPath("/")
                    .setHttpOnly(true)
                    .build();
            exchange.getResponseHeaders().add("Set-Cookie",out.toCookieHeaderString());
        }
    }

}