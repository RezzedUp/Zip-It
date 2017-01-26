package com.rezzedup.zip;

import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Stream;

public class DirectoryZipper extends Thread
{
    private final ConcurrentLinkedDeque<String> workLog = new ConcurrentLinkedDeque<>();
    private final FileCounter counter = new FileCounter();
    
    private boolean skip = true;
    private boolean isRecursive = true;
    
    private final File source;
    private final File output;
    private final Filter<String> filter;
    
    public static Builder into(File outputDirectory)
    {
        return new Builder(outputDirectory);
    }
    
    private DirectoryZipper(File sourceDirectory, File outputFile, Filter<String> filter)
    {
        this.source = sourceDirectory;
        this.output = outputFile;
        this.filter = filter;
        
        if (!sourceDirectory.isDirectory())
        {
            // TODO: clarify to user
            return;
        }
        
        if (outputFile.isFile())
        {
            // TODO: clarify to user
            return;
        }
        
        this.skip = false;
    }
    
    @Override
    public synchronized void start()
    {
        if (this.skip)
        {
            return;
        }
        super.start();
    }
    
    private boolean accepts(Path path)
    {
        return !Files.isDirectory(path);
        // TODO: add more filters
    }
    
    private Stream<Path> getPaths()
    {
        try
        {
            return (this.isRecursive) ? Files.walk(source.toPath()) : Files.list(source.toPath());
        }
        catch (IOException io)
        {
            io.printStackTrace();
        }
        return new ArrayList<Path>().stream();
    }
    
    @Override
    public void run()
    {
        if (this.skip)
        {
            return;
        }
        
        this.workLog.push("Calculating total files...");
        this.counter.totalFiles = getPaths().count();
    
        ZipUtil.pack(source, output, name ->
        {
            if (this.filter.accepts(name))
            {
                String entry = source.getPath() + "/" + name;
                this.workLog.push("  Added: " + entry);
                this.counter.completedFiles += 1;
                return entry;
            }
            else
            {
                this.workLog.push("  Skipped: " + name);
                this.counter.skippedFiles += 1;
                return null;
            }
        });
    }
    
    public synchronized long getPercentComplete()
    {
        return (this.counter.completedFiles + this.counter.skippedFiles) / this.counter.totalFiles;
    }
    
    public synchronized String getWorkingName()
    {
        return this.source + " -> " + this.output;
    }
    
    public synchronized Deque<String> getLatestWork()
    {
        Deque<String> work = new LinkedList<>(this.workLog);
        this.workLog.clear();
        return work;
    }
    
    // FileCounter
    
    private static class FileCounter
    {
        private long totalFiles = 0;
        private long completedFiles = 0;
        private long skippedFiles = 0;
    }
    
    // ZipBuilder
    
    public static class Builder
    {
        private boolean isRecursive = true;
        
        private String prefix = null;
        private String date = null;
        private File sourceDirectory = null;
        private Filter<String> filter = null;
        
        private final File outputDirectory;
        
        public Builder(File into)
        {
            this.outputDirectory = into;
        }
        
        private void validate(String purpose, String value)
        {
            if (value == null)
            {
                throw new IllegalStateException("Missing a value for " + purpose);
            }
            else if (!value.matches("^[a-zA-Z0-9_-]+$"))
            {
                throw new IllegalStateException("Cannot use '" + value + "' for " + purpose);
            }
        }
        
        private void validate(String purpose, Object object)
        {
            if (object == null)
            {
                throw new IllegalStateException("Missing a value for " + purpose);
            }
        }
        
        public Builder withPrefix(String prefix)
        {
            this.prefix = prefix;
            return this;
        }
        
        public Builder withDate(String date)
        {
            this.date = date;
            return this;
        }
        
        public Builder withSourceDirectory(File source)
        {
            this.sourceDirectory = source;
            return this;
        }
        
        public Builder withFilter(Filter<String> filter)
        {
            this.filter = filter;
            return this;
        }
        
        public DirectoryZipper build()
        {
            validate("withPrefix", this.prefix);
            validate("withDate", this.date);
            validate("withSourceDirectory", this.sourceDirectory);
            validate("output", this.outputDirectory);
            validate("filter", this.filter);
            
            String sourceName = sourceDirectory.getName().replaceAll("\\.|\\" + File.separator, "").replaceAll(" ", "_");
            
            String source = (sourceName.isEmpty()) ? "" : "." + sourceName;
            
            String name = prefix + "." + date + source + ".zip";
            File output = new File(outputDirectory, name);
            
            DirectoryZipper zip = new DirectoryZipper(sourceDirectory, output, filter);
            
            zip.isRecursive = this.isRecursive;
            
            return zip;
        }
    }
}
