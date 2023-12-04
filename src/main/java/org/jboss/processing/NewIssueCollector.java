package org.jboss.processing;

import com.atlassian.jira.rest.client.api.domain.Issue;
import io.quarkus.logging.Log;
import org.apache.camel.component.jira.JiraEndpoint;
import org.apache.camel.component.jira.consumer.NewIssuesConsumer;
import org.jboss.jql.JqlBuilder;
import org.jboss.processing.state.EveryIssueState;
import org.jboss.processing.state.SingleIssueState;
import org.jboss.query.IssueStatus;
import org.jboss.query.SearchQuery;

import java.util.ArrayList;
import java.util.List;

public class NewIssueCollector extends NewIssuesConsumer implements Executable<EveryIssueState> {

    private static final State state = new State();

    private NewIssueCollector(JiraEndpoint jiraEndpoint) {
        super(jiraEndpoint, exchange -> {
            Issue issue = exchange.getIn().getBody(Issue.class);
            if (issue != null) {
                state.issueStates.add(new SingleIssueState(issue));
            }
        });
    }

    private static final class State {
        List<SingleIssueState> issueStates = new ArrayList<>();
    }

    public static NewIssueCollector getInstance(JiraEndpoint jiraEndpoint) {
        SearchQuery searchQuery = SearchQuery.builder().projects("WFLY").status(IssueStatus.NEW).assigneeEmpty()
                .build();
        jiraEndpoint.setJql(JqlBuilder.build(searchQuery));
        return new NewIssueCollector(jiraEndpoint);
    }

    @Override
    public EveryIssueState execute() throws Exception {
        int issuesCount = doPoll();
        Log.infof("Found number of issues %d", issuesCount);
        return new EveryIssueState(state.issueStates);
    }
}
