package ktt.lib.httpserver.server;

import com.sun.net.httpserver.*;
import com.sun.net.httpserver.HttpExchange;
import ktt.lib.httpserver.http.HTTPCode;
import ktt.lib.httpserver.http.RequestMethod;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

abstract class SimpleHttpExchangeImpl {

    static SimpleHttpExchange createSimpleHttpExchange(final HttpExchange exchange){
        return new SimpleHttpExchange() {

            private final HttpServer httpServer;
            private final HttpExchange httpExchange;

            private final URI URI;
            private final InetSocketAddress publicAddr, localAddr;

            private final HttpContext httpContext;
            private final HttpPrincipal httpPrincipal;
            private final String protocol, authority, scheme, context, query, fragment;

            private final Headers requestHeaders;
            private final RequestMethod requestMethod;

            private final String rawGet;
            private final HashMap<String,String> getMap;
            private final boolean hasGet;

            private final String rawPost;
            private final HashMap postMap;
            private final boolean hasPost;

            private boolean closed = false;

            private final Function<String,HashMap<String,String>> parseWwwFormEnc = s -> {
                            final LinkedHashMap<String,String> OUT = new LinkedHashMap<>();
                            final String[] pairs = s.split("&");

                            for(final String pair : pairs){
                                if(pair.contains("=")){
                                    final String[] kv = pair.split("=");
                                    OUT.put(
                                        URLDecoder.decode(kv[0],StandardCharsets.UTF_8),
                                        kv.length == 2 ? URLDecoder.decode(kv[1],StandardCharsets.UTF_8) : null
                                    );
                                }
                            }
                            return OUT;
                        };

            {
                httpServer = (httpContext = exchange.getHttpContext()).getServer();
                httpExchange = exchange;
            //
                URI = httpExchange.getRequestURI();
                publicAddr  = httpExchange.getRemoteAddress();
                localAddr   = httpExchange.getLocalAddress();
            //
                httpPrincipal = httpExchange.getPrincipal();
                protocol    = httpExchange.getProtocol();
                authority   = URI.getAuthority();
                scheme      = URI.getScheme();
                context     = URI.getPath();
                query       = URI.getQuery();
                fragment    = URI.getFragment();
            //
                requestHeaders = httpExchange.getRequestHeaders();
                switch(exchange.getRequestMethod()){
                    case "GET":
                        requestMethod = RequestMethod.GET; break;
                    case "HEAD":
                        requestMethod = RequestMethod.HEAD; break;
                    case "POST":
                        requestMethod = RequestMethod.POST; break;
                    case "PUT":
                        requestMethod = RequestMethod.PUT; break;
                    case "DELETE":
                        requestMethod = RequestMethod.DELETE; break;
                    case "CONNECT":
                        requestMethod = RequestMethod.CONNECT; break;
                    case "OPTIONS":
                        requestMethod = RequestMethod.OPTIONS; break;
                    case "TRACE":
                        requestMethod = RequestMethod.TRACE; break;
                    case "PATCH":
                        requestMethod = RequestMethod.PATCH; break;
                    default:
                        requestMethod = RequestMethod.UNSUPPORTED; break;
                }
            //
                hasGet = (rawGet = URI.getQuery()) != null;
                getMap = parseWwwFormEnc.apply(rawGet);
            //
                String OUT;
                final InputStream IN = httpExchange.getRequestBody();

                try(final Scanner scanner = new Scanner(IN,StandardCharsets.UTF_8)){
                    OUT = scanner.useDelimiter("\\A").next();
                    IN.close();
                }catch(IOException e){
                    OUT = null;
                }

                hasPost = (rawPost = OUT) != null;

                // region todo: optimize this
                final String content_type = requestHeaders.getFirst("Content-type");
                if(content_type != null && content_type.startsWith("multipart/form-data")){
                    final Pattern hpat = Pattern.compile("(.*): (.*?)(?:$|; )(.*)");
                    final Pattern vpat = Pattern.compile("(.*?)=\"(.*?)\"(?:; |$)");

                    final String webkitBoundary = content_type.substring(content_type.indexOf("; boundary=") + 11);
                    final String startBoundary = "--" + webkitBoundary + "\r\n";
                    final String endBoundary = "--" + webkitBoundary + "--\r\n";

                    postMap = new HashMap<>();
                    /* Parse multipart/form-data */ {
                        final String[] pairs = OUT.replace(endBoundary,"").split(Pattern.quote(startBoundary));
                        for (String pair : pairs) {
                            final HashMap<String, HashMap> postHeaders = new HashMap<>();
                            if (pair.contains("\r\n\r\n")) {
                                final String[] headers = pair.substring(0, pair.indexOf("\r\n\r\n")).split("\r\n");

                                for (String header : headers) {
                                    final HashMap hmap = new HashMap<>();
                                    final HashMap<String, String> val = new HashMap<>();

                                    final Matcher hmatch = hpat.matcher(header);
                                    if (hmatch.find()) {
                                        final Matcher vmatch = vpat.matcher(hmatch.group(3));
                                        while (vmatch.find()) {
                                            val.put(vmatch.group(1), vmatch.group(2));
                                        }
                                        hmap.put("header-name", hmatch.group(1));
                                        hmap.put("header-value", hmatch.group(2));
                                        hmap.put("parameter", val);
                                    }
                                    postHeaders.put((String) hmap.get("header-name"), hmap);
                                }

                                final HashMap row = new HashMap();
                                row.put("headers",postHeaders);
                                row.put("value",pair.substring(pair.indexOf("\r\n\r\n")+4,pair.lastIndexOf("\r\n")));

                                postMap.put(
                                    ((HashMap<String, String>) postHeaders.get("Content-Disposition").get("parameter")).get("name"),
                                    row
                                );

                            }
                        }
                    }
                }else{
                    postMap = parseWwwFormEnc.apply(rawPost);
                }
                // endregion
            }

        //

            @Override
            public final HttpServer getHttpServer(){
                return httpServer;
            }

            @Override
            public final HttpExchange getHttpExchange(){
                return httpExchange;
            }

        //

            @Override
            public final URI getURI(){
                return URI;
            }

            @Override
            public final InetSocketAddress getPublicAddress(){
                return publicAddr;
            }

            @Override
            public final InetSocketAddress getLocalAddress(){
                return localAddr;
            }

        //

            @Override
            public final HttpContext getHttpContext(){
                return httpContext;
            }

            @Override
            public final HttpPrincipal getHttpPrincipal(){
                return httpPrincipal;
            }

            @Override
            public final String getProtocol(){
                return protocol;
            }

            @Override
            public final String getScheme(){
                return scheme;
            }

            @Override
            public final String getAuthority(){
                return authority;
            }

            /**
             * @deprecated use {@link #getRawGet()} instead
             * @return
             */
            @Override @Deprecated
            public final String getQuery(){
                return query;
            }

            @Override
            public final String getContext(){
                return context;
            }

            @Override
            public final String getFragment(){
                return fragment;
            }

            //

            @Override
            public final Headers getRequestHeaders(){
                return requestHeaders;
            }

            @Override
            public final RequestMethod getRequestMethod(){
                return requestMethod;
            }

        //

            @Override
            public final String getRawGet(){
                return rawGet;
            }

            @Override
            public final HashMap<String, String> getGetMap(){
                return getMap;
            }

            @Override
            public final boolean hasGet(){
                return hasGet;
            }

        //

            @Override
            public final String getRawPost(){
                return rawPost;
            }

            @Override @SuppressWarnings("rawtypes")
            public final HashMap getPostMap(){
                return postMap;
            }

            @Override
            public final boolean hasPost(){
                return hasPost;
            }

        //

            @Override
            public final Headers getResponseHeaders(){
                return httpExchange.getResponseHeaders();
            }

            @Override
            public final int getResponseCode(){
                return httpExchange.getResponseCode();
            }

        //

            @Override
            public synchronized final void sendResponseHeaders(final int code, final long length) throws IOException{
                if(closed) return;
                httpExchange.sendResponseHeaders(code, length);
                close();
            }

            @Override
            public synchronized final void send(final int responseCode) throws IOException{
                sendResponseHeaders(responseCode,0);
            }

            @Override
            public synchronized final void send(final byte[] response) throws IOException{
                send(response, HTTPCode.HTTP_OK,false);
            }

            @Override
            public void send(final byte[] response, final boolean gzip) throws IOException{
                send(response,HTTPCode.HTTP_OK,gzip);
            }

            @Override
            public synchronized final void send(final byte[] response, final int responseCode) throws IOException {
                send(response,responseCode,false);
            }

            @Override
            public void send(final byte[] response, final int responseCode, final boolean gzip) throws IOException{
                if(closed) return;

                if(gzip){
                    exchange.getResponseHeaders().set("Accept-Encoding","gzip");
                    exchange.getResponseHeaders().set("Content-Encoding","gzip");
                    exchange.getResponseHeaders().set("Connection","keep-alive");
                    sendResponseHeaders(responseCode, 0);
                    GZIPOutputStream OUT = new GZIPOutputStream(exchange.getResponseBody());
                    OUT.write(response);
                    OUT.finish();
                    OUT.close();
                    exchange.getResponseBody().close();

                    close();
                }else{
                    sendResponseHeaders(responseCode,response.length);
                    final OutputStream OUT = exchange.getResponseBody();
                    OUT.write(response);
                    OUT.close();

                    close();
                }
            }

            @Override
            public synchronized final void send(final String response) throws IOException{
                send(response.getBytes(StandardCharsets.UTF_8),HTTPCode.HTTP_OK,false);
            }

            @Override
            public void send(final String response, final boolean gzip) throws IOException{
                send(response.getBytes(StandardCharsets.UTF_8),HTTPCode.HTTP_OK,gzip);
            }

            @Override
            public synchronized final void send(final String response, final int responseCode) throws IOException{
                send(response.getBytes(StandardCharsets.UTF_8),responseCode,false);
            }

            @Override
            public void send(final String response, final int responseCode, final boolean gzip) throws IOException{
                send(response.getBytes(StandardCharsets.UTF_8),responseCode,gzip);
            }

            //

            @Override
            public synchronized final void close(){
                if(!closed){
                    exchange.close();
                    closed = true;
                }
            }

        //

            @Override
            public final Object getAttribute(final String name){
                return httpExchange.getAttribute(name);
            }

            @Override
            public synchronized final void setAttribute(final String name, final Object value){
                httpExchange.setAttribute(name, value);
            }

        };
    }

    private static String getContext(final String path){
        final String linSlash = path.toLowerCase().replace("\\","/");
        final String seSlash = (!linSlash.startsWith("/") ? "/" : "") + linSlash + (!linSlash.endsWith("/") ? "/" : "");
        return seSlash.substring(0,seSlash.length()-1);
    }

}