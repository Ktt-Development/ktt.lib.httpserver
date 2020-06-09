package com.kttdevelopment.simplehttpserver.handler;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Represents a directory in the {@link FileHandler}. Applications do not use this class.
 *
 * @see FileHandler
 * @see FileEntry
 * @since 02.00.00
 * @version 03.05.01
 * @author Ktt Development
 */
class DirectoryEntry {

    private final File directory;
    private final FileHandlerAdapter adapter;
    private final ByteLoadingOption loadingOption;
    private final boolean isWalkthrough;

    private final Map<String,FileEntry> preloadedFiles = new ConcurrentHashMap<>(); // preload/watch-load only
    private final Path directoryPath;

    /**
     * Create a directory entry.
     *
     * @param directory directory to represent
     * @param adapter how to process the bytes in {@link #getBytes(String)}
     * @param loadingOption how to handle the initial file loading
     * @param isWalkthrough whether to use sub-directories or not
     * @throws UncheckedIOException failure to walk through directory or failure to start watch service
     *
     * @see FileBytesAdapter
     * @see ByteLoadingOption
     * @since 03.05.00
     * @author Ktt Development
     */
    DirectoryEntry(final File directory, final FileHandlerAdapter adapter, final ByteLoadingOption loadingOption, final boolean isWalkthrough){
        this.directory = directory;
        this.adapter = adapter;
        this.loadingOption = loadingOption;
        this.isWalkthrough = isWalkthrough;

        directoryPath = directory.toPath();

        if(loadingOption == ByteLoadingOption.WATCHLOAD){
            if(!isWalkthrough){ // load top level only
                for(final File file : Objects.requireNonNullElse(directory.listFiles(), new File[0])) // initial population
                    if(!file.isDirectory()) // File#isFile does not work
                        try{
                            preloadedFiles.put(
                                getContext(adapter.getName(file)),
                                new FileEntry(file, adapter, ByteLoadingOption.WATCHLOAD, true)
                            );
                        }catch(final UncheckedIOException ignored){ }
                try{ // create watch service for top level directory
                    createWatchService(directoryPath, createWatchServiceConsumer(directoryPath));
                }catch(final IOException e){
                    throw new UncheckedIOException(e);
                }
            }else{ // load top and sub levels
                try{
                    Files.walkFileTree(directoryPath, new SimpleFileVisitor<>() {
                        @Override
                        public final FileVisitResult preVisitDirectory(final Path path, final BasicFileAttributes attrs) throws IOException{
                            createWatchService(path, createWatchServiceConsumer(path));
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public final FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs){
                            final File file = path.toFile();
                            final String relative = directoryPath.relativize(path.getParent()).toString(); // attach the relative path (parent) to the adapted file name

                            try{
                                preloadedFiles.put(
                                    joinContext(relative, adapter.getName(file)),
                                    new FileEntry(file, adapter, loadingOption, true)
                                );
                            }catch(final UncheckedIOException ignored){ }

                            return FileVisitResult.CONTINUE;
                        }
                    });
                }catch(final IOException e){
                    throw new UncheckedIOException(e);
                }
            }
        }else if(loadingOption == ByteLoadingOption.PRELOAD){
            if(!isWalkthrough){ // load top level only
                final File[] listFiles = Objects.requireNonNullElse(directory.listFiles(), new File[0]);
                for(final File file : listFiles) // initial population
                    if(!file.isDirectory())
                        try{
                            preloadedFiles.put(
                                getContext(adapter.getName(file)),
                                new FileEntry(file, adapter, ByteLoadingOption.PRELOAD)
                            );
                        }catch(final UncheckedIOException ignored){ }
            }else{ // load top and sub levels
                try{
                    Files.walkFileTree(directoryPath, new SimpleFileVisitor<>() {
                        @Override
                        public final FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs){
                            final File file = path.toFile();
                            final String relative = directoryPath.relativize(path.getParent()).toString(); // attach the relative path (parent) to the adapted file name

                            try{
                                preloadedFiles.put(
                                    joinContext(relative,adapter.getName(file)),
                                    new FileEntry(file, adapter, ByteLoadingOption.PRELOAD)
                                );
                            }catch(final RuntimeException ignored){ }

                            return FileVisitResult.CONTINUE;
                        }
                    });
                }catch(final IOException e){
                    throw new UncheckedIOException(e);
                }
            }
        }
    }


    private final Map<Path,AtomicBoolean> watchService = new ConcurrentHashMap<>();

    private void createWatchService(final Path path, final Consumer<WatchEvent<?>> consumer) throws IOException{
        final WatchService service = FileSystems.getDefault().newWatchService();
        path.register(service,ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY);

        final AtomicBoolean stop = new AtomicBoolean(false);

        new Thread(() -> {
            WatchKey key;
            try{
                while((key = service.take()) != null){
                    for(WatchEvent<?> event : key.pollEvents())
                        consumer.accept(event);
                    key.reset();
                    if(stop.get())
                        break;
                }
            }catch(final InterruptedException ignored){ }
        }).start();

        watchService.put(path,stop);
    }

    @SuppressWarnings({"StatementWithEmptyBody", "SpellCheckingInspection"})
    private Consumer<WatchEvent<?>> createWatchServiceConsumer(final Path path){
        return (WatchEvent<?> event) -> {
            try{
                final Path relTarg = directoryPath.resolve((Path) event.context()); // only the file name (this method is flawed!)
                final File relFile = relTarg.toFile(); // only the file name (this method if flawed!)
                final WatchEvent.Kind<?> type = event.kind();

                final String top2sub = getContext(directoryPath.relativize(path).toString()); // the relative path between the top level directory and sub directory
                final String context = joinContext(top2sub,adapter.getName(relFile)); // the file key
                final File file = new File(directoryPath + joinContext(top2sub,relFile.getName())); // the actual referable file
                final Path target = file.toPath();

                if(!file.isDirectory()) // File#isFile does not work
                    if(type == ENTRY_CREATE)
                        preloadedFiles.put(context, new FileEntry(file,adapter,ByteLoadingOption.WATCHLOAD,true));
                    else if(type == ENTRY_DELETE)
                        preloadedFiles.remove(context);
                    else if(type == ENTRY_MODIFY)
                        Objects.requireNonNull(preloadedFiles.get(context)).reloadBytes();
                    else; // prevent ambiguous else with below
                else if(isWalkthrough) // only add/remove if walkthrough
                   if(type == ENTRY_CREATE)
                       try{
                           createWatchService(target, createWatchServiceConsumer(target));
                       }catch(final IOException ignored){ }
                   else if(type == ENTRY_DELETE){
                       Objects.requireNonNull(watchService.get(relTarg)).set(true);
                       watchService.remove(relTarg);
                   }

                   preloadedFiles.get(context).reloadBytes();

            }catch(final ClassCastException | NullPointerException ignored){ }
        };
    }

//

    /**
     * Returns the directory being referenced
     *
     * @return referenced directory
     *
     * @see #getPreloadedFiles()
     * @see #getFile(String)
     * @since 02.00.00
     * @author Ktt Development
     */
    public final File getDirectory(){
        return directory;
    }

    /**
     * Returns the directories and their associated contexts. <b>Preload only.</b>
     *
     * @return preloaded files
     *
     * @see #getDirectory()
     * @see #getFile(String)
     * @since 02.00.00
     * @author Ktt Development
     */
    public Map<String, FileEntry> getPreloadedFiles(){
        return Collections.unmodifiableMap(preloadedFiles);
    }

    /**
     * Returns the file at the associated path. <b>Preload only.</b>
     *
     * @param path context to check
     * @return file associated with that context
     *
     * @see #getDirectory()
     * @see #getPreloadedFiles()
     * @since 02.00.00
     * @author Ktt Development
     */
    @SuppressWarnings("SpellCheckingInspection")
    public final File getFile(final String path){
        final String relative = getContext(path);
        if(loadingOption != ByteLoadingOption.LIVELOAD){
            return preloadedFiles.get(relative).getFile();
        }else{ // file is a reference, existence of file does not matter
            final String dabs = directory.getAbsolutePath();
            final File parentFile = new File(dabs + relative).getParentFile();
            final String pabs = parentFile.getAbsolutePath();

            // if is in top level directory (both cases) or if is a child of the directory folder (walk case)
            // null otherwise
            return pabs.equals(dabs) || (isWalkthrough && pabs.startsWith(dabs)) ? new File(dabs + relative) : null;
        }
    }

    /**
     * Returns the file's bytes after the {@link FileHandlerAdapter} was used or null if it was not found or failed to read. <b>Preload only.</b>
     *
     * @param path context to check
     * @return processed file bytes
     *
     * @see FileHandlerAdapter
     * @since 02.00.00
     * @author Ktt Development
     */
    public final byte[] getBytes(final String path){
        final String rel = getContext(path);
        if(loadingOption != ByteLoadingOption.LIVELOAD ){ // find preloaded bytes
            return preloadedFiles.get(rel).getBytes(); // already adapted
        }else{
            try{
                final File file = Objects.requireNonNull(getFile(path)); // find if file allowed
                return adapter.getBytes(file,Files.readAllBytes(file.toPath())); // adapt bytes here
            }catch(final NullPointerException | IOException ignored){
                return null;
            }
        }
    }

    /**
     * Returns the file's byte loading option.
     *
     * @return file loading option
     *
     * @see ByteLoadingOption
     * @since 03.05.00
     * @author Ktt Development
     */
    public final ByteLoadingOption getLoadingOption(){
        return loadingOption;
    }

    /**
     * Returns if the directory uses inner folders and files
     *
     * @return if directory uses inner folders and files
     *
     * @since 02.00.00
     * @author Ktt Development
     */
    public final boolean isWalkthrough(){
        return isWalkthrough;
    }

//

    // leading slash only
    private static String getContext(final String path){
        final String linSlash = path.replace("\\","/");
        if(linSlash.equals("/")) return "/";
        final String seSlash = (!linSlash.startsWith("/") ? "/" : "") + linSlash + (!linSlash.endsWith("/") ? "/" : "");
        return seSlash.substring(0,seSlash.length()-1);
    }

    private static String joinContext(final String... paths){
        final StringBuilder OUT = new StringBuilder();
        for(final String path : paths){
            final String context = getContext(path);
            OUT.append(context.isEmpty() || context.equals("/") || context.equals("\\") ? "" : context);
        }
        return OUT.toString();
    }

//


    @Override
    public String toString(){
        return
            "DirectoryEntry"    + '{' +
            "directory"         + '=' +     directory       + ", " +
            "adapter"           + '=' +     adapter         + ", " +
            "loadingOption"     + '=' +     loadingOption   + ", " +
            "isWalkthrough"     + '=' +     isWalkthrough   + ", " +
            "preloadedFiles"    + '=' +     preloadedFiles  +
            '}';
    }

}
