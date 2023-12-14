package org.jboss.testing;

import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This interface is used for testing purposes of Injected instance.
 * Due to treating the command classes annotated with
 * the {@code picocli.CommandLine.Command} as a bean and using CDI specific
 * features such as {@code jakarta.annotation.PostConstruct}.
 * Otherwise, an ambiguous injection would happen.
 */
@Qualifier
@Retention(RUNTIME)
@Target({ FIELD, TYPE })
public @interface JiraCommand {
}
