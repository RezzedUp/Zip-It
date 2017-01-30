package com.rezzedup.zip;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class Main
{
    public static final String VERSION = "1.0.0";
    public static final ProgramOptions OPTIONS = new ProgramOptions();
    
    public static class ProgramOptions
    {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String prefix = "backup";
        File output = new File("zip");
        RegexPathFilter filter = new RegexPathFilter();
        File specificSource = null;
        File workingDirectory = new File(".");
        String jar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
    }
    
    public static void main(String[] args)
    {
        if (!adjustProgramOptions(args))
        {
            return;
        }
    
        Print.option("Date (-d)", OPTIONS.date);
        Print.option("Prefix (-p)", OPTIONS.prefix);
        Print.option("Output Directory (-o)", OPTIONS.output.toString());
        
        String source = 
            (OPTIONS.specificSource == null) 
                ? "All Directories" 
                : (OPTIONS.specificSource == OPTIONS.workingDirectory) 
                    ? "Local Root Directory" 
                    : OPTIONS.specificSource.toString();
        
        Print.option("Source (-s)", source);
        Print.option("File Exclusion Filters", "\n  " + String.join("\n  ", OPTIONS.filter.rawInput));
        
        String consent = Print.prompt("Is this acceptable? (Y/n)");
        
        if (!consent.isEmpty() && !consent.matches("(?i)^y.*"))
        {
            Print.line("Cancelled.");
            return;
        }
        
        if (OPTIONS.output.mkdirs())
        {
            Print.notice("Created: " + OPTIONS.output);
        }
        
        if (OPTIONS.specificSource == null)
        {
            File[] directories = OPTIONS.workingDirectory.listFiles(File::isDirectory);
            
            if (directories == null)
            {
                Print.line("FATAL ERROR: Working directory is not a directory?");
                return;
            }
            
            for (File dir : directories)
            {
                zip(dir);
            }
            zipWorkingDirectory();
        }
        else 
        {
            if (OPTIONS.specificSource == OPTIONS.workingDirectory)
            {
                zipWorkingDirectory();
            }
            else
            {
                zip(OPTIONS.specificSource);
            }
        }
        
        Print.line("Complete.");
    }
    
    private static void zip(File directory)
    {
        prepare(directory).build().run();
    }
    
    private static void zipWorkingDirectory()
    {
        prepare(OPTIONS.workingDirectory).recursive(false).build().run();
    }
    
    private static DirectoryZipper.Builder prepare(File directory)
    {
        return DirectoryZipper.of(directory).output(OPTIONS.output).prefix(OPTIONS.prefix).date(OPTIONS.date).filter(OPTIONS.filter);
    }
    
    private static boolean pathIsValid(String path)
    {
        boolean invalid = path.matches("^(\\.|\\/|\\\\).*");
        
        if (invalid)
        {
            Print.notice("Invalid path", path);
        }
        return invalid;
    }
    
    private static boolean adjustProgramOptions(String[] args)
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
        
        Option overridePrefix = 
            Option.builder("p")
                .longOpt("prefix")
                .desc("Override the output zip's prefix.\n")
                .hasArg()
                .argName("prefix")
            .build();
        options.addOption(overridePrefix);
        
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
            Print.line(e.getMessage());
            return false;
        }
        
        for (Option option : line.getOptions())
        {
            switch (option.getOpt())
            {
                case "v":
                {
                    Print.line("ZipIt v" + VERSION + " by RezzedUp");
                    return false;
                }
                case "h":
                {
                    HelpFormatter help = new HelpFormatter();
                    help.setWidth(120);
                    help.printHelp("java -jar " + OPTIONS.jar + " [options]\noptions:", options);
                    return false;
                }
                case "xx":
                {
                    OPTIONS.filter.clearFilters();
                }
                case "x":
                {
                    for (String value : option.getValues())
                    {
                        OPTIONS.filter.addWildcardFilter(value);
                    }
                    break;
                }
                case "xregx":
                {
                    OPTIONS.filter.clearFilters();
                }
                case "regx":
                {
                    for (String value : option.getValues())
                    {
                        OPTIONS.filter.addRegexFilter(value);
                    }
                    break;
                }
                case "p":
                {
                    String prefix = option.getValue();
                    
                    if (prefix.matches("^\\w[\\w-]*$"))
                    {
                        OPTIONS.prefix = prefix;
                    }
                    else 
                    {
                        Print.notice("Invalid prefix", prefix);
                        return false;
                    }
                    break;
                }
                case "d":
                {
                    String date = option.getValue();
    
                    if (date.matches("[0-9]{4}(-|_)?[0-9]{1,2}(-|_)?[0-9]{1,2}"))
                    {
                        OPTIONS.date = date;
                    }
                    else
                    {
                        Print.notice("Invalid date", date);
                        return false;
                    }
                    break;
                }
                case "o":
                {
                    String value = option.getValue();
    
                    if (!pathIsValid(value)) 
                    {
                        return false; 
                    }
                    
                    OPTIONS.output = new File(option.getValue());
                    break;
                }
                case "s":
                {
                    String value = option.getValue();
    
                    if (value == null)
                    {
                        OPTIONS.specificSource = OPTIONS.workingDirectory;
                    }
                    else
                    {
                        if (!pathIsValid(value))
                        {
                            return false;
                        }
                        File source = new File(value);
                        
                        if (!source.isDirectory())
                        {
                            Print.notice("Invalid path", value + " is not a directory.");
                            return false;
                        }
                        OPTIONS.specificSource = source;
                    }
                    break;
                }
                default:
                {
                    Print.line("Found: " + option.getOpt() + " with: " + option.getValue());
                }
            }
        }
        return true;
    }
    
    public static class RegexPathFilter implements Filter<String>
    {
        private final List<String> rawInput = new ArrayList<>();
        private final List<String> patterns = new ArrayList<>();
        
        public RegexPathFilter()
        {
            // Default filters
            addWildcardFilter("*.zip").addWildcardFilter("*.db");
        }
        
        public void clearFilters()
        {
            this.patterns.clear();
        }
        
        public RegexPathFilter addWildcardFilter(String filter)
        {
            // explanation - http://stackoverflow.com/a/24337692
            String regex = ("(?i)\\Q" + filter + "\\E").replace("*", "\\E.*\\Q");
            add(filter, regex);
            return this;
        }
        
        public RegexPathFilter addRegexFilter(String filter)
        {
            add(filter, "(?i)" + filter);
            return this;
        }
    
        @SuppressWarnings("ResultOfMethodCallIgnored")
        private void add(String raw, String pattern)
        {
            try
            {
                "test.demo".matches(pattern);
                this.patterns.add(pattern);
                this.rawInput.add(raw);
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
