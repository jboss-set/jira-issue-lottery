package org.jboss.set.wrappers;

import com.atlassian.jira.rest.client.api.StatusCategory;
import com.atlassian.jira.rest.client.api.domain.Attachment;
import com.atlassian.jira.rest.client.api.domain.BasicComponent;
import com.atlassian.jira.rest.client.api.domain.BasicPriority;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.BasicVotes;
import com.atlassian.jira.rest.client.api.domain.BasicWatchers;
import com.atlassian.jira.rest.client.api.domain.ChangelogGroup;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.IssueLink;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Operations;
import com.atlassian.jira.rest.client.api.domain.Resolution;
import com.atlassian.jira.rest.client.api.domain.Status;
import com.atlassian.jira.rest.client.api.domain.Subtask;
import com.atlassian.jira.rest.client.api.domain.TimeTracking;
import com.atlassian.jira.rest.client.api.domain.User;
import com.atlassian.jira.rest.client.api.domain.Version;
import com.atlassian.jira.rest.client.api.domain.Worklog;
import org.jboss.query.IssueStatus;
import org.joda.time.DateTime;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class IssueWrapper extends Issue {

    public static final String URI_ID_TEMPLATE = "https://example.jira.com/project/%d";

    public IssueWrapper(String summary, Long id, String project, IssueStatus status, Collection<String> components)
            throws URISyntaxException {
        this(summary, new URI(URI_ID_TEMPLATE.formatted(id)), null, id,
                new BasicProject(new URI(URI_ID_TEMPLATE.formatted(id)), project, id, project), null,
                new Status(null, id, status.toString(), null, null, new StatusCategory(null, null, null, null, null)), null,
                null, null, null, null, null, null, null, null,
                null, null, components.stream().map(component -> new BasicComponent(null, id, component, null)).collect(
                        Collectors.toList()),
                null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private IssueWrapper(String summary, URI self, String key, Long id, BasicProject project, IssueType issueType,
            Status status, String description, BasicPriority priority, Resolution resolution,
            Collection<Attachment> attachments, User reporter, User assignee, DateTime creationDate, DateTime updateDate,
            DateTime dueDate, Collection<Version> affectedVersions, Collection<Version> fixVersions,
            Collection<BasicComponent> components, TimeTracking timeTracking, Collection<IssueField> issueFields,
            Collection<Comment> comments, URI transitionsUri, Collection<IssueLink> issueLinks, BasicVotes votes,
            Collection<Worklog> worklogs, BasicWatchers watchers, Iterable<String> expandos, Collection<Subtask> subtasks,
            Collection<ChangelogGroup> changelog, Operations operations, Set<String> labels) {
        super(summary, self, key, id, project, issueType, status, description, priority, resolution, attachments, reporter,
                assignee, creationDate, updateDate, dueDate, affectedVersions, fixVersions, components, timeTracking,
                issueFields, comments, transitionsUri, issueLinks, votes, worklogs, watchers, expandos, subtasks, changelog,
                operations, labels);
    }
}
