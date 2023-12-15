package org.jboss;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.component.jira.JiraComponent;
import org.apache.camel.component.jira.JiraConfiguration;
import org.apache.camel.component.jira.JiraEndpoint;

@ApplicationScoped
public class JiraEndpointProducer {

    public JiraEndpoint getEndpoint(String uri, JiraComponent component, JiraConfiguration configuration) {
        return new JiraEndpoint(uri, component, configuration);
    }
}
