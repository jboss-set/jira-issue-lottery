package org.jboss.processing.state;

import com.atlassian.jira.rest.client.api.domain.BasicComponent;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.User;
import org.jboss.query.IssueStatus;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This class
 */
public class SingleIssueState extends BasicIssue {

    private final User assignee;
    private final BasicProject project;
    private final Collection<BasicComponent> components;
    private final IssueStatus issueStatus;

    public SingleIssueState(Issue issue) {
        super(issue.getSelf(), issue.getKey(), issue.getId());
        this.issueStatus = IssueStatus.getInstance(issue.getStatus().getName());
        // Consider copying using Constructors if necessary
        this.assignee = issue.getAssignee();
        this.project = issue.getProject();
        this.components = new ArrayList<>();
        issue.getComponents().forEach(components::add);
    }

    public BasicProject getProject() {
        return project;
    }

    public Collection<BasicComponent> getComponents() {
        return components;
    }

    public IssueStatus getIssueStatus() {
        return issueStatus;
    }

    public User getAssignee() {
        return assignee;
    }
}
