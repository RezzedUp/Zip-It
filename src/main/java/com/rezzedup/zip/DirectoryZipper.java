package com.rezzedup.zip;

import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

public class DirectoryZipper
{
    private final FileCounter counter = new FileCounter();
    
    private boolean skip = false;
    private boolean isRecursive = true;
    
    private final File source;
    private final File tempOutput;
    private final File completeOutput;
    private final Filter<String> filter;
    private final String entryPath;
    
    public static Builder of(File sourceDirectory)
    {
        return new Builder(sourceDirectory);
    }
    
    private DirectoryZipper(File sourceDirectory, File outputDirectory, String outputName, Filter<String> filter)
    {
        this.source = sourceDirectory;
        this.tempOutput = new File(outputDirectory, outputName + ".temp");
        this.completeOutput = new File(outputDirectory, outputName + ".zip");
        this.filter = filter;
    
        String path = this.source.getPath().replaceAll("^(\\.|\\/)*", "");
    
        if (!path.isEmpty() && !path.endsWith("/"))
        {
            path += "/";
        }
        this.entryPath = path;
        
        if (!sourceDirectory.isDirectory())
        {
            skip("No such directory '" + sourceDirectory + "'");
            return;
        }
    
        if (completeOutput.isFile())
        {
            skip("Output zip file already exists");
            return;
        }
        
        if (tempOutput.isFile())
        {
            Print.notice("Temp output file exists. Deleting: '" + tempOutput + "'");
            tempOutput.delete();
            
            if (tempOutput.isFile())
            {
                skip("Unable to delete temporary output file.");
                return;
            }
        }
    }
    
    private void skip(String reason)
    {
        this.skip = true;
        Print.notice("Skipping " + source.getName(), reason);
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
    
    public void run()
    {
        if (this.skip)
        {
            return;
        }
        
        Print.line(Ansi.Cyan.and(Ansi.HighIntensity).colorize("Zipping: '" + this.source + "' --> '" + this.completeOutput + "'"));
        
        Print.status("  Calculating total files...");
        this.counter.totalFiles = getPaths().count();
        Print.option("  Found", this.counter.totalFiles + " files");
    
        if (this.isRecursive)
        {
            ZipUtil.pack(this.source, this.tempOutput, this::accept);
        }
        else
        {
            try
            {
                this.tempOutput.createNewFile();
                // create an empty zip...
                ZipOutputStream stream = new ZipOutputStream(new FileOutputStream(this.tempOutput));
                stream.close();
            }
            catch (IOException io)
            {
                io.printStackTrace();
                return;
            }
            
            getPaths().forEach(path -> 
            {
                if (Files.isDirectory(path)) { return; }
                
                File file = path.toFile();
                String name = file.getName();
                
                if (accept(name) == null) { return; }
                
                ZipUtil.addEntry(this.tempOutput, name, file);
            });
        }
        
        Print.clarify("  Renamed " + tempOutput.getName() + " to " + completeOutput.getName());
        
        tempOutput.renameTo(completeOutput);
        
        Print.status("  Done.");
    }
    
    private String accept(String name)
    {
        String entry = this.entryPath + name;
        
        if (this.filter.accepts(entry))
        {
            Print.line("  Adding: " + entry);
            
            if (this.counter.completedFiles % 10 == 0)
            {
                Print.clarify("  --> " + getPercentComplete() + "% Complete");
            }
            
            this.counter.completedFiles += 1;
            return entry;
        }
        else
        {
            Print.notice("  Skipping", entry);
            this.counter.skippedFiles += 1;
            return null;
        }
    }
    
    public long getPercentComplete()
    {
        double visited = this.counter.completedFiles + this.counter.skippedFiles;
        double percent = visited / (double) this.counter.totalFiles;
        return (long) (percent * 100);
    }
    
    public String getWorkingName()
    {
        return this.source + " -> " + this.completeOutput;
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
        private File outputDirectory = null;
        private Filter<String> filter = null;
        
        private final File sourceDirectory;
        
        public Builder(File from)
        {
            this.sourceDirectory = from;
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
        
        public Builder prefix(String prefix)
        {
            this.prefix = prefix;
            return this;
        }
        
        public Builder date(String date)
        {
            this.date = date;
            return this;
        }
        
        public Builder output(File output)
        {
            this.outputDirectory = output;
            return this;
        }
        
        public Builder filter(Filter<String> filter)
        {
            this.filter = filter;
            return this;
        }
        
        public Builder recursive(boolean toggle)
        {
            this.isRecursive = toggle;
            return this;
        }
        
        public DirectoryZipper build()
        {
            validate("prefix", this.prefix);
            validate("date", this.date);
            validate("source", this.sourceDirectory);
            validate("output", this.outputDirectory);
            validate("filter", this.filter);
            
            String source = sourceDirectory.getName().replaceAll("\\.|\\" + File.separator, "").replaceAll(" ", "_");
            String name = prefix + "." + date + ((source.isEmpty()) ? "" : "." + source);
            
            DirectoryZipper zip = new DirectoryZipper(sourceDirectory, outputDirectory, name, filter);
            
            zip.isRecursive = this.isRecursive;
            
            return zip;
        }
    }
}
