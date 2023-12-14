package org.jboss.set.draw;

import com.atlassian.httpclient.api.HttpClient;
import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.atlassian.util.concurrent.Promise;
import io.atlassian.util.concurrent.Promises;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jira.JiraEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.apache.camel.spi.Registry;
import org.jboss.JiraEndpointProducer;
import org.jboss.JiraIssueLotteryCommand;
import org.jboss.config.LotteryConfig;
import org.jboss.config.LotteryConfigProducer;
import org.jboss.draw.Lottery;
import org.jboss.query.IssueStatus;
import org.jboss.set.wrappers.IssueWrapper;
import org.jboss.testing.JiraCommand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.camel.component.jira.JiraConstants.JIRA_REST_CLIENT_FACTORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
public class LotteryDrawingTest extends CamelQuarkusTestSupport {

    @Inject
    @JiraCommand
    JiraIssueLotteryCommand app;

    @Inject
    MockMailbox mailbox;

    @InjectMock
    private JiraEndpointProducer jiraEndpointProducer;

    @Inject
    ObjectMapper objectMapper;

    @InjectMock
    LotteryConfigProducer lotteryConfigProducer;

    private final JiraRestClient jiraRestClient = Mockito.mock(JiraRestClient.class);
    private final JiraRestClientFactory jiraRestClientFactory = Mockito.mock(JiraRestClientFactory.class);
    private final SearchRestClient searchRestClient = Mockito.mock(SearchRestClient.class);
    private final JiraEndpoint jiraEndpoint = Mockito.mock(JiraEndpoint.class);

    @Inject
    CamelContext camelContext;

    private static List<Issue> issues;
    private static List<org.jboss.draw.entities.Issue> ourIssues;

    @BeforeAll
    static void init() {
        try {
            issues = List.of(
                    new IssueWrapper("Issue", 1L, "WFLY", IssueStatus.NEW, Set.of("Documentation")),
                    new IssueWrapper("Issue", 2L, "RESTEASY", IssueStatus.NEW, Set.of("Logging")),
                    new IssueWrapper("Issue", 3L, "WELD", IssueStatus.NEW, Set.of("Logging")),
                    new IssueWrapper("Issue", 4L, "WELD", IssueStatus.NEW, Set.of("Logging")));
            ourIssues = issues.stream().map(org.jboss.draw.entities.Issue::new).toList();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    protected void doBeforeEach() {
        Mockito.reset(lotteryConfigProducer);
        mailbox.clear();
    }

    @Override
    protected void bindToRegistry(Registry registry) {
        registry.bind(JIRA_REST_CLIENT_FACTORY, jiraRestClientFactory);
    }

    public void setMocks() {
        when(jiraRestClientFactory.create(any(), (AuthenticationHandler) any())).thenReturn(jiraRestClient);
        when(jiraRestClientFactory.create(any(), (HttpClient) any())).thenReturn(jiraRestClient);

        when(jiraEndpointProducer.getEndpoint(any(), any(), any())).thenReturn(jiraEndpoint);

        when(jiraEndpoint.getClient()).thenReturn(jiraRestClient);
        when(jiraEndpoint.getCamelContext()).thenReturn(camelContext);
        when(jiraRestClient.getSearchClient()).thenReturn(searchRestClient);

        when(searchRestClient.searchJql(any(), any(), any(), any())).then(new Answer<Promise<SearchResult>>() {

            boolean visited = false;

            @Override
            public Promise<SearchResult> answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (!visited) {
                    visited = true;
                    return Promises.promise(new SearchResult(0, 50, issues.size(), issues));
                }
                return Promises.promise(new SearchResult(0, 50, 0, Collections.emptyList()));
            }
        });
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        setMocks();
        return camelContext;
    }

    @Test
    public void testAssigningOneIssue() throws Exception {
        String email = "The-Huginn@thehuginn.com";
        String configFile = """
                delay: P14D
                participants:
                  - username: The-Huginn@thehuginn.com
                    days: [MONDAY]
                    projects:
                      - project: WFLY
                        components: [Logging, Documentation]
                        maxIssues: 5""";

        LotteryConfig testLotteryConfig = objectMapper.readValue(configFile, LotteryConfig.class);
        when(lotteryConfigProducer.getLotteryConfig()).thenReturn(testLotteryConfig);

        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(app);
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute();
        assertEquals(0, exitCode);

        List<Mail> sent = mailbox.getMailsSentTo(email);
        assertEquals(1, sent.size());
        assertEquals(Lottery.EMAIL_SUBJECT, sent.get(0).getSubject());
        assertEquals(Lottery.createEmailText(email, List.of(ourIssues.get(0))), sent.get(0).getText());
    }

    @Test
    public void testAssigningAllIssues() throws Exception {
        String email = "The-Huginn@thehuginn.com";
        String configFile = """
                delay: P14D
                participants:
                  - username: The-Huginn@thehuginn.com
                    days: [MONDAY]
                    projects:
                      - project: WFLY
                        components: [Documentation]
                        maxIssues: 5
                      - project: RESTEASY
                        components: [Logging]
                        maxIssues: 5""";

        LotteryConfig testLotteryConfig = objectMapper.readValue(configFile, LotteryConfig.class);
        when(lotteryConfigProducer.getLotteryConfig()).thenReturn(testLotteryConfig);

        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(app);
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute();
        assertEquals(0, exitCode);

        List<Mail> sent = mailbox.getMailsSentTo(email);
        assertEquals(1, sent.size());
        assertEquals(Lottery.EMAIL_SUBJECT, sent.get(0).getSubject());
        assertEquals(Lottery.createEmailText(email, List.of(ourIssues.get(0), ourIssues.get(1))), sent.get(0).getText());
    }

    @Test
    public void testAssigningNoIssues() throws Exception {
        String email = "The-Huginn@thehuginn.com";
        String configFile = """
                delay: P14D
                participants:
                  - username: The-Huginn@thehuginn.com
                    days: [MONDAY]
                    projects:
                      - project: WFLY
                        components: [Logging]
                        maxIssues: 5
                      - project: RESTEASY
                        components: [Documentation]
                        maxIssues: 5""";

        LotteryConfig testLotteryConfig = objectMapper.readValue(configFile, LotteryConfig.class);
        when(lotteryConfigProducer.getLotteryConfig()).thenReturn(testLotteryConfig);

        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(app);
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute();
        assertEquals(0, exitCode);

        List<Mail> sent = mailbox.getMailsSentTo(email);
        assertEquals(0, sent.size());
    }

    @Test
    public void testAssigningAllIssuesToTwoParticipants() throws Exception {
        String configFile = """
                delay: P14D
                participants:
                  - username: The-Huginn@thehuginn.com
                    days: [MONDAY]
                    projects:
                      - project: WFLY
                        components: [Documentation]
                        maxIssues: 5
                  - username: xstefank@redhat.com
                    days: [MONDAY]
                    projects:
                      - project: RESTEASY
                        components: [Logging]
                        maxIssues: 5""";

        LotteryConfig testLotteryConfig = objectMapper.readValue(configFile, LotteryConfig.class);
        when(lotteryConfigProducer.getLotteryConfig()).thenReturn(testLotteryConfig);

        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(app);
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute();
        assertEquals(0, exitCode);

        for (Map.Entry<String, org.jboss.draw.entities.Issue> userIssue : List.of(
                Map.entry("The-Huginn@thehuginn.com", ourIssues.get(0)),
                Map.entry("xstefank@redhat.com", ourIssues.get(1)))) {
            List<Mail> sent = mailbox.getMailsSentTo(userIssue.getKey());
            assertEquals(1, sent.size());
            assertEquals(Lottery.EMAIL_SUBJECT, sent.get(0).getSubject());
            assertEquals(Lottery.createEmailText(userIssue.getKey(), List.of(userIssue.getValue())),
                    sent.get(0).getText());
        }
    }

    @Test
    public void testAssigningMaximumIssues() throws Exception {
        String email = "The-Huginn@thehuginn.com";
        String configFile = """
                delay: P14D
                participants:
                  - username: The-Huginn@thehuginn.com
                    days: [MONDAY]
                    projects:
                      - project: WELD
                        components: [Logging]
                        maxIssues: 1""";

        LotteryConfig testLotteryConfig = objectMapper.readValue(configFile, LotteryConfig.class);
        when(lotteryConfigProducer.getLotteryConfig()).thenReturn(testLotteryConfig);

        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(app);
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute();
        assertEquals(0, exitCode);

        List<Mail> sent = mailbox.getMailsSentTo(email);
        assertEquals(1, sent.size());
        assertEquals(Lottery.EMAIL_SUBJECT, sent.get(0).getSubject());
        assertEquals(Lottery.createEmailText(email, List.of(ourIssues.get(2))), sent.get(0).getText());
    }
}
