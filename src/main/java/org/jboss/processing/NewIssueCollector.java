package org.jboss.processing;

import com.atlassian.jira.rest.client.api.domain.Issue;
import io.quarkus.arc.Arc;
import io.quarkus.logging.Log;
import org.apache.camel.component.jira.JiraEndpoint;
import org.apache.camel.component.jira.consumer.NewIssuesConsumer;
import org.jboss.draw.Lottery;
import org.jboss.jql.JqlBuilder;
import org.jboss.processing.state.EveryIssueState;
import org.jboss.processing.state.SingleIssueState;
import org.jboss.query.IssueStatus;
import org.jboss.query.SearchQuery;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NewIssueCollector extends NewIssuesConsumer implements Executable {

    private static final State state = new State();

    private final Lottery lottery;

    private NewIssueCollector(JiraEndpoint jiraEndpoint) {
        super(jiraEndpoint, exchange -> {
            Issue issue = exchange.getIn().getBody(Issue.class);
            if (issue != null) {
                state.issueStates.add(new SingleIssueState(issue));
            }
        });
        lottery = Arc.container().instance(Lottery.class).get();
    }

    private static final class State {
        List<SingleIssueState> issueStates = new CopyOnWriteArrayList<>();
    }

    public static NewIssueCollector getInstance(JiraEndpoint jiraEndpoint) {
        SearchQuery searchQuery = SearchQuery.builder().projects("WFLY").status(IssueStatus.NEW).assigneeEmpty()
                .build();
        jiraEndpoint.setJql(JqlBuilder.build(searchQuery));
        return new NewIssueCollector(jiraEndpoint);
    }

    @Override
    public void execute() throws Exception {
        int issuesCount = doPoll();
        Log.infof("Found number of issues %d", issuesCount);
        EveryIssueState issues = new EveryIssueState(state.issueStates);
        lottery.run(issues);
    }
}
