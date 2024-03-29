package org.jboss.set.draw;

import io.quarkus.mailer.Mail;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.jboss.set.LotteryCommand;
import org.jboss.set.config.LotteryConfig;
import org.jboss.set.config.LotteryConfigProducer;
import org.jboss.set.draw.entities.Issue;
import org.jboss.set.query.IssueStatus;
import org.jboss.set.wrappers.IssueWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@QuarkusTest
public class LotteryDrawingTest extends AbstractCommandTest {

    @InjectMock
    protected LotteryConfigProducer lotteryConfigProducer;

    @Inject
    LotteryCommand app;

    @Override
    protected List<com.atlassian.jira.rest.client.api.domain.Issue> setupIssues() {
        try {
            return List.of(
                    new IssueWrapper("Issue", 1L, "WFLY", IssueStatus.NEW, Set.of("Documentation")),
                    new IssueWrapper("Issue", 2L, "RESTEASY", IssueStatus.NEW, Set.of("Logging")),
                    new IssueWrapper("Issue", 3L, "WELD", IssueStatus.NEW, Set.of("Logging")),
                    new IssueWrapper("Issue", 4L, "WELD", IssueStatus.NEW, Set.of("Logging", "Documentation")));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testAssigningOneIssue() throws Exception {
        String email = "Tadpole@thehuginn.com";
        String configFile = """
                delay: P14D
                participants:
                  - email: %s
                    maxIssues: 5
                    projects:
                      - project: WFLY
                        components: [Logging, Documentation]
                        maxIssues: 5""".formatted(email);

        LotteryConfig testLotteryConfig = objectMapper.readValue(configFile, LotteryConfig.class);
        when(lotteryConfigProducer.getLotteryConfig()).thenReturn(testLotteryConfig);

        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(app);
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute();
        assertEquals(0, exitCode);

        List<Mail> sent = mailbox.getMailsSentTo(email);
        assertEquals(1, sent.size());
        Assertions.assertEquals(Lottery.EMAIL_SUBJECT.formatted("Tadpole"), sent.get(0).getSubject());
        assertEquals(Lottery.createEmailText(email, List.of(ourIssues.get(0))), sent.get(0).getHtml());
    }

    @Test
    public void testAssigningOneIssueOnlyProjectDefined() throws Exception {
        String email = "Tadpole@thehuginn.com";
        String configFile = """
                delay: P14D
                participants:
                  - email: %s
                    maxIssues: 5
                    projects:
                      - project: WFLY
                        maxIssues: 5""".formatted(email);

        LotteryConfig testLotteryConfig = objectMapper.readValue(configFile, LotteryConfig.class);
        when(lotteryConfigProducer.getLotteryConfig()).thenReturn(testLotteryConfig);

        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(app);
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute();
        assertEquals(0, exitCode);

        List<Mail> sent = mailbox.getMailsSentTo(email);
        assertEquals(1, sent.size());
        assertEquals(Lottery.createEmailText(email, List.of(ourIssues.get(0))), sent.get(0).getHtml());
    }

    @Test
    public void testAssigningAllIssues() throws Exception {
        String email = "Tadpole@thehuginn.com";
        String configFile = """
                delay: P14D
                participants:
                  - email: %s
                    maxIssues: 10
                    projects:
                      - project: WFLY
                        components: [Documentation]
                        maxIssues: 5
                      - project: RESTEASY
                        components: [Logging]
                        maxIssues: 5""".formatted(email);

        LotteryConfig testLotteryConfig = objectMapper.readValue(configFile, LotteryConfig.class);
        when(lotteryConfigProducer.getLotteryConfig()).thenReturn(testLotteryConfig);

        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(app);
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute();
        assertEquals(0, exitCode);

        List<Mail> sent = mailbox.getMailsSentTo(email);
        assertEquals(1, sent.size());
        assertEquals(Lottery.createEmailText(email, List.of(ourIssues.get(0), ourIssues.get(1))), sent.get(0).getHtml());
    }

    @Test
    public void testAppendingRepositoryForUnsubcription() throws Exception {
        String email = "Tadpole@thehuginn.com";
        String configFile = """
                delay: P14D
                participants:
                  - email: %s
                    maxIssues: 5
                    projects:
                      - project: WFLY
                        components: [Documentation]
                        maxIssues: 5""".formatted(email);

        LotteryConfig testLotteryConfig = objectMapper.readValue(configFile, LotteryConfig.class);
        when(lotteryConfigProducer.getLotteryConfig()).thenReturn(testLotteryConfig);

        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(app);
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute();
        assertEquals(0, exitCode);

        List<Mail> sent = mailbox.getMailsSentTo(email);
        assertEquals(1, sent.size());
        String emailText = sent.get(0).getHtml();

        //Checks only if email message is correctly formatted. The URL is only for test purpose and may be invalid.
        String expectedUrl = "https://github.com/jboss-set/jira-issue-lottery/blob/main/.github/jira-issue-lottery.yml";
        assertTrue(emailText.contains(expectedUrl));
    }

    @Test
    public void testExtractingUsername() throws Exception {
        String email = "Tadpole@thehuginn.com";
        String configFile = """
                delay: P14D
                participants:
                  - email: %s
                    maxIssues: 5
                    projects:
                      - project: WFLY
                        components: [Documentation]
                        maxIssues: 5""".formatted(email);

        LotteryConfig testLotteryConfig = objectMapper.readValue(configFile, LotteryConfig.class);
        when(lotteryConfigProducer.getLotteryConfig()).thenReturn(testLotteryConfig);

        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(app);
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute();
        assertEquals(0, exitCode);

        List<Mail> sent = mailbox.getMailsSentTo(email);
        assertEquals(1, sent.size());
        // Check for correct parsing of username from email
        assertTrue(sent.get(0).getHtml().contains("Hi Tadpole,"));
        assertTrue(sent.get(0).getSubject().contains("Tadpole"));
    }

    @Test
    public void testAssigningNoIssues() throws Exception {
        String email = "Tadpole@thehuginn.com";
        String configFile = """
                delay: P14D
                participants:
                  - email: %s
                    maxIssues: 10
                    projects:
                      - project: WFLY
                        components: [Logging]
                        maxIssues: 5
                      - project: RESTEASY
                        components: [Documentation]
                        maxIssues: 5""".formatted(email);

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
                  - email: Tadpole@thehuginn.com
                    maxIssues: 5
                    projects:
                      - project: WFLY
                        components: [Documentation]
                        maxIssues: 5
                  - email: xstefank@redhat.com
                    maxIssues: 5
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

        for (Map.Entry<String, Issue> userIssue : List.of(
                Map.entry("Tadpole@thehuginn.com", ourIssues.get(0)),
                Map.entry("xstefank@redhat.com", ourIssues.get(1)))) {
            List<Mail> sent = mailbox.getMailsSentTo(userIssue.getKey());
            assertEquals(1, sent.size());
            assertEquals(Lottery.EMAIL_SUBJECT.formatted(Lottery.getUsername(userIssue.getKey())), sent.get(0).getSubject());
            assertEquals(Lottery.createEmailText(userIssue.getKey(), List.of(userIssue.getValue())),
                    sent.get(0).getHtml());
        }
    }

    @Test
    public void testAssigningMaximumIssues() throws Exception {
        String email = "Tadpole@thehuginn.com";
        String configFile = """
                delay: P14D
                participants:
                  - email: %s
                    maxIssues: 5
                    projects:
                      - project: WELD
                        components: [Logging]
                        maxIssues: 1""".formatted(email);

        LotteryConfig testLotteryConfig = objectMapper.readValue(configFile, LotteryConfig.class);
        when(lotteryConfigProducer.getLotteryConfig()).thenReturn(testLotteryConfig);

        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(app);
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute();
        assertEquals(0, exitCode);

        List<Mail> sent = mailbox.getMailsSentTo(email);
        assertEquals(1, sent.size());
        assertEquals(Lottery.createEmailText(email, List.of(ourIssues.get(2))), sent.get(0).getHtml());
    }

    @Test
    public void testAssigningPartialComponentsHitFromConfig() throws Exception {
        String email = "Tadpole@thehuginn.com";
        String configFile = """
                delay: P14D
                participants:
                  - email: %s
                    maxIssues: 5
                    projects:
                      - project: WELD
                        components: [Logging]
                        maxIssues: 2""".formatted(email);

        LotteryConfig testLotteryConfig = objectMapper.readValue(configFile, LotteryConfig.class);
        when(lotteryConfigProducer.getLotteryConfig()).thenReturn(testLotteryConfig);

        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(app);
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute();
        assertEquals(0, exitCode);

        List<Mail> sent = mailbox.getMailsSentTo(email);
        assertEquals(1, sent.size());
        assertEquals(Lottery.createEmailText(email, List.of(ourIssues.get(2), ourIssues.get(3))), sent.get(0).getHtml());
    }

    @Test
    public void testAssigningPartialComponentsHitFromIssue() throws Exception {
        String email = "Tadpole@thehuginn.com";
        String configFile = """
                delay: P14D
                participants:
                  - email: %s
                    maxIssues: 5
                    projects:
                      - project: WELD
                        components: [Logging, Documentation]
                        maxIssues: 2""".formatted(email);

        LotteryConfig testLotteryConfig = objectMapper.readValue(configFile, LotteryConfig.class);
        when(lotteryConfigProducer.getLotteryConfig()).thenReturn(testLotteryConfig);

        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(app);
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute();
        assertEquals(0, exitCode);

        List<Mail> sent = mailbox.getMailsSentTo(email);
        assertEquals(1, sent.size());
        assertEquals(Lottery.createEmailText(email, List.of(ourIssues.get(2), ourIssues.get(3))), sent.get(0).getHtml());
    }

    @Test
    public void testAssigningWithNoPartialComponentsHit() throws Exception {
        String email = "Tadpole@thehuginn.com";
        String configFile = """
                delay: P14D
                participants:
                  - email: %s
                    maxIssues: 5
                    projects:
                      - project: WELD
                        components: [Documentation]
                        maxIssues: 2""".formatted(email);

        LotteryConfig testLotteryConfig = objectMapper.readValue(configFile, LotteryConfig.class);
        when(lotteryConfigProducer.getLotteryConfig()).thenReturn(testLotteryConfig);

        StringWriter sw = new StringWriter();
        CommandLine cmd = new CommandLine(app);
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute();
        assertEquals(0, exitCode);

        List<Mail> sent = mailbox.getMailsSentTo(email);
        assertEquals(1, sent.size());
        assertEquals(Lottery.createEmailText(email, List.of(ourIssues.get(3))), sent.get(0).getHtml());
    }
}
