package org.jboss.draw.entities;

import com.atlassian.jira.rest.client.api.domain.BasicComponent;
import org.jboss.processing.state.SingleIssueState;

import java.net.URI;
import java.util.List;
import java.util.stream.StreamSupport;

public class Issue {
    private final Long id;
    private final URI uri;
    private final String project;
    private final List<String> components;
    private Participant assignee;

    public Issue(SingleIssueState issue) {
        this(issue.getId(),
                issue.getSelf(),
                issue.getProject().getKey(),
                issue.getComponents().stream().map(BasicComponent::getName).toList());
    }

    public Issue(com.atlassian.jira.rest.client.api.domain.Issue issue) {
        this(issue.getId(),
                issue.getSelf(),
                issue.getProject().getKey(),
                StreamSupport.stream(issue.getComponents().spliterator(), false).map(BasicComponent::getName).toList());
    }

    public Issue(Long id, URI uri, String project, List<String> components) {
        this.id = id;
        this.uri = uri;
        this.project = project;
        this.components = components;
    }

    public Long getId() {
        return id;
    }

    public URI getUri() {
        return uri;
    }

    public String getProject() {
        return project;
    }

    public List<String> getComponents() {
        return components;
    }

    public Participant getAssignee() {
        return assignee;
    }

    /**
     * It's intentionally package private so only {@code Participant}
     * can invoke this method.
     */
    void setAssignee(Participant assignee) {
        this.assignee = assignee;
    }
}
