package com.rezzedup.zip;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.PatternSyntaxException;

public class Main
{
    public static final String VERSION = "1.0.0";
    public static final ProgramOptions OPTIONS = new ProgramOptions();
    public static final PrintStream OUT = System.out;
    
    public static class ProgramOptions
    {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String prefix = "backup";
        File output = new File("zip");
        String specificSource = null;
        RegexPathFilter filter = new RegexPathFilter().addWildcardFilter("*.zip").addWildcardFilter("*.db");
        File workingDirectory = new File(".");
        String jar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
    }
    
    public static void main(String[] args)
    {
        adjustProgramOptions(args);
    
        OUT.format("Date (-d): %s%n", OPTIONS.date);
        OUT.format("Prefix (-p): %s%n", OPTIONS.prefix);
        OUT.format("Output Directory (-o): %s%n", OPTIONS.output);
        
        if (OPTIONS.specificSource == null)
        {
            OUT.println("Source (-s): All Directories");
        }
        else 
        {
            if (OPTIONS.specificSource.isEmpty())
            {
                OUT.println("Source (-s): Local Root Directory");
            }
            else 
            {
                OUT.format("Source (-s): %s%n", OPTIONS.specificSource);
            }
        }
        
        OUT.format("File Exclusion Filters: %s%n", String.join(", ", OPTIONS.filter.patterns));
        OUT.format("Threads: 5%n");
        
        OUT.format("Is this acceptable? (Y/n) ");
        
        String consent = new Scanner(System.in).nextLine().trim();
        
        if (!consent.isEmpty() && !consent.matches("(?i)^y.*"))
        {
            OUT.println("Cancelled.");
            return;
        }
        
        if (OPTIONS.output.mkdirs())
        {
            OUT.println("Created: " + OPTIONS.output);

        }
        
        DirectoryZipper zip = 
            DirectoryZipper.into(OPTIONS.output)
                .withPrefix(OPTIONS.prefix)
                .withDate(OPTIONS.date)
                .withSourceDirectory(new File("plugins"))
                .withFilter(OPTIONS.filter.copy())
            .build();
        
        zip.start();
        
        while (zip.isAlive())
        {
            String name = zip.getWorkingName();
            Deque<String> work = zip.getLatestWork();
            
            System.out.println("Latest work from: " + name);
            
            for (int i = 0; i < 3; i++)
            {
                try
                {
                    System.out.println(work.pop());
                }
                catch (NoSuchElementException e)
                {
                    System.out.println("--- That's it so far ---");
                    break;
                }
            }
            
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                System.out.println("Failure.");
                return;
            }
        }
        
        OUT.println("Done.");
    }
    
    private static void adjustProgramOptions(String[] args)
    {
        Options options = new Options();
    
        Option printVersion = 
            Option.builder("v")
                .longOpt("version")
                .desc("Prints the program version.\n")
            .build();
        options.addOption(printVersion);
        
        Option printHelp = 
            Option.builder("h")
                .longOpt("help")
                .desc("Prints this usage information.\n")
            .build();
        options.addOption(printHelp);
    
        Option explicitExclusionList = 
            Option.builder("xx")
                .longOpt("explicit-exclude")
                .desc
                (
                    "A list into wildcard exclusion filters. (see '-x' for example.)\n" +
                    "This will also remove all previously defined filters.\n"
                )
                .hasArgs()
                .argName("filters")
            .build();
        options.addOption(explicitExclusionList);
        
        Option exclusionList = 
            Option.builder("x")
                .longOpt("exclude")
                .desc("A list into wildcard exclusion filters.\nexample: -x *.ext file* w*card \"*.thing\"\n")
                .hasArgs()
                .argName("filters")
            .build();
        options.addOption(exclusionList);
    
        Option explicitRegexExclusionList = 
            Option.builder("xregx")
                .longOpt("explicit-regex-exclude")
                .desc
                (
                    "A list into regex-pattern exclusion filters. (see '-regx' for example.)\n" +
                    "This will also remove all previously defined filters.\n"
                )
                .hasArgs()
                .argName("filters")
            .build();
        options.addOption(explicitRegexExclusionList);
        
        Option regexExclusionList = 
            Option.builder("regx")
                .longOpt("regex-exclude")
                .desc("A list into regex-pattern exclusion filters.\nexample: -regx \"^.*\\.(db|zip)$\"\n")
                .hasArgs()
                .argName("filters")
            .build();
        options.addOption(regexExclusionList);
        
        Option overrideDate = 
            Option.builder("d")
                .longOpt("date")
                .desc("Override the output zip's date.\n")
                .hasArg()
                .argName("date")
            .build();
        options.addOption(overrideDate);
        
        Option overrideOutput =
            Option.builder("o")
                .longOpt("output")
                .desc("Override the default output directory.\n")
                .hasArg()
                .argName("dir")
            .build();
        options.addOption(overrideOutput);
        
        Option specifySource =
            Option.builder("s")
                .longOpt("source")
                .desc("Specify a specific source directory.\n")
                .hasArg()
                .optionalArg(true)
                .argName("dir")
            .build();
        options.addOption(specifySource);
    
        CommandLineParser parser = new DefaultParser();
        CommandLine line;
        
        try
        {
            line = parser.parse(options, args);
        }
        catch (ParseException e)
        {
            System.out.println(e.getMessage());
            return;
        }
        
        for (Option option : line.getOptions())
        {
            switch (option.getOpt())
            {
                case "v":
                    System.out.println("ZipIt v" + VERSION + " by RezzedUp");
                    return;
                    
                case "h":
                    HelpFormatter help = new HelpFormatter();
                    help.setWidth(120);
                    help.printHelp("java -jar " + OPTIONS.jar + " [options]\noptions:", options);
                    return;
                    
                case "xx":
                    OPTIONS.filter.clearFilters();
                    
                case "x":
                    for (String value : option.getValues())
                    {
                        OPTIONS.filter.addWildcardFilter(value);
                    }
                    break;
                    
                case "xregx":
                    OPTIONS.filter.clearFilters();
                    
                case "regx":
                    for (String value : option.getValues())
                    {
                        OPTIONS.filter.addRegexFilter(value);
                    }
                    break;
                    
                case "d":
                    String date = option.getValue();
                    
                    if (date.matches("[0-9]{4}(-|_)?[0-9]{1,2}(-|_)?[0-9]{1,2}"))
                    {
                        OPTIONS.date = date;
                    }
                    else 
                    {
                        System.out.println("'" + date + "' is not a valid date.");
                        return;
                    }
                    break;
                    
                case "o":
                    OPTIONS.output = new File(option.getValue());
                    break;
                    
                case "s":
                    String value = option.getValue();
                    OPTIONS.specificSource = (value == null) ? "" : value;
                    break;
                    
                default:
                    System.out.println("Found: " + option.getOpt() + " with: " + option.getValue());
            }
        }
    }
    
    public static class RegexPathFilter implements Filter<String>
    {
        private final List<String> patterns = new ArrayList<>();
        
        public RegexPathFilter() {}
        
        public RegexPathFilter(RegexPathFilter filter)
        {
            this.patterns.addAll(filter.patterns);
        }
        
        public RegexPathFilter copy()
        {
            return new RegexPathFilter(this);
        }
        
        public void clearFilters()
        {
            this.patterns.clear();
        }
        
        public RegexPathFilter addWildcardFilter(String filter)
        {
            // explanation - http://stackoverflow.com/a/24337692
            String regex = ("(?i)\\Q" + filter + "\\E").replace("*", "\\E.*\\Q");
            add(regex);
            return this;
        }
        
        public RegexPathFilter addRegexFilter(String filter)
        {
            add("(?i)" + filter);
            return this;
        }
    
        @SuppressWarnings("ResultOfMethodCallIgnored")
        private void add(String pattern)
        {
            try
            {
                "test.demo".matches(pattern);
                this.patterns.add(pattern);
            }
            catch (PatternSyntaxException e)
            {
                // TODO: remove stacktrace and clarify to user about failed pattern
                e.printStackTrace();
            }
        }
        
        @Override
        public boolean accepts(String path)
        {
            for (String pattern : patterns)
            {
                if (path.matches(pattern))
                {
                    return false;
                }
            }
            return true;
        }
    }
}
