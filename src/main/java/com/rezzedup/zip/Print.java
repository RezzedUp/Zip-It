package com.rezzedup.zip;

import java.util.Scanner;

public final class Print
{
    private Print() {}
    
    public static String prompt(String prompt)
    {
        System.out.print(prompt + " ");
        return new Scanner(System.in).nextLine().trim();
    }
    
    public static void line(String text)
    {
        System.out.println(text);
    }
    
    public static void format(String template, Object ... objects)
    {
        Print.line(String.format(template, objects));
    }
    
    public static void option(String name, String value)
    {
        Print.line(Ansi.Green.and(Ansi.HighIntensity).colorize(name + ": ") + value);
    }
    
    public static void notice(String notice)
    {
        Print.line(Ansi.Yellow.colorize(notice));
    }
    
    public static void notice(String notice, String clarity)
    {
        Print.line(Ansi.Yellow.colorize(notice) + ": " + clarity);
    }
    
    public static void status(String status)
    {
        Print.line(Ansi.Cyan.colorize(status));
    }
    
    public static void clarify(String text)
    {
        Print.line(Ansi.White.and(Ansi.HighIntensity).colorize(text));
    }
}
