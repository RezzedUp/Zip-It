package com.rezzedup.zip;

public interface Filter<T>
{
    boolean accepts(T t);
}
