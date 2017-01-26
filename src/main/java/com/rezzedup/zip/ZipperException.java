package com.rezzedup.zip;

public class ZipperException extends RuntimeException
{
    public ZipperException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
    
    public ZipperException(String message)
    {
        super(message);
    }
    
    public ZipperException(Throwable throwable)
    {
        super(throwable);
    }
}
