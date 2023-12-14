package org.jboss.processing;

/**
 * Even if it is basically the same as {@code java.lang.Runnable} interface,
 * due to interference we need a separate interface mimicking it.
 */
public interface Executable {
    void execute() throws Exception;
}
