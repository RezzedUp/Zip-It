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
        String specificSource = null;
        RegexPathFilter filter = new RegexPathFilter();
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
                : (OPTIONS.specificSource.isEmpty()) 
                    ? "Local Root Directory" 
                    : OPTIONS.specificSource;
        
        Print.option("Source (-s)", source);
        Print.option("File Exclusion Filters:\n  ", String.join("\n  ", OPTIONS.filter.patterns));
        
        String consent = Print.prompt("Is this acceptable? (Y/n)");
        
        if (!consent.isEmpty() && !consent.matches("(?i)^y.*"))
        {
            System.out.println("Cancelled.");
            return;
        }
        
        if (OPTIONS.output.mkdirs())
        {
            Print.notice("Created: " + OPTIONS.output);
        }
        
        DirectoryZipper zip = 
            DirectoryZipper.of(new File("plugins"))
                .output(OPTIONS.output)
                .prefix(OPTIONS.prefix)
                .date(OPTIONS.date)
                .filter(OPTIONS.filter)
            .build();
        
        zip.run();
        
        System.out.println("Done.");
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
            return false;
        }
        
        for (Option option : line.getOptions())
        {
            switch (option.getOpt())
            {
                case "v":
                    System.out.println("ZipIt v" + VERSION + " by RezzedUp");
                    return false;
                    
                case "h":
                    HelpFormatter help = new HelpFormatter();
                    help.setWidth(120);
                    help.printHelp("java -jar " + OPTIONS.jar + " [options]\noptions:", options);
                    return false;
                    
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
                        return false;
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
