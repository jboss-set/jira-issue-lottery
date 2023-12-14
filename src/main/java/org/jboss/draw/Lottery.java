package org.jboss.draw;

import io.quarkus.arc.Unremovable;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.config.LotteryConfig;
import org.jboss.config.LotteryConfigProducer;
import org.jboss.draw.entities.Issue;
import org.jboss.draw.entities.Participant;
import org.jboss.processing.state.EveryIssueState;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Unremovable
@ApplicationScoped
public class Lottery {

    @Inject
    private LotteryConfigProducer lotteryConfigProducer;

    @Inject
    private Mailer mailer;

    private static final Pattern USERNAME_FROM_EMAIL = Pattern.compile("\\s+(\\w+)@");
    public static final String EMAIL_SUBJECT = "Subject";
    private static final String EMAIL_BODY = """
            Hi %s,

            your issues chosen for you by the JIRA lottery:

            %s

            Regards with love, Jira issue lottery bot""";

    public void run(EveryIssueState collectedIssues) {
        LotteryConfig lotteryConfig = lotteryConfigProducer.getLotteryConfig();
        // retrieves participants from LotteryConfig
        List<Participant> participants = lotteryConfig.participants().parallelStream()
                .map(participant -> {
                    Map<String, Tuple2<Integer, Set<String>>> collectedProjects = participant.projects().stream()
                            .map(project -> Map.entry(project.project(),
                                    Tuple2.of(project.participation().maxIssues(), project.components())))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    return new Participant(participant.username(), collectedProjects);
                })
                .collect(Collectors.toList());
        // retrieves issues from LotteryConfig
        List<Issue> issues = collectedIssues.getIssueStates().parallelStream()
                .map(Issue::new)
                .toList();

        Collections.shuffle(participants);
        int lastParticipant = 0;
        for (Issue issue : issues) {
            // break this loop if the same participant is encountered again
            for (int i = (lastParticipant + 1) % participants.size(); true; i = (i + 1) % participants.size()) {
                Participant participant = participants.get(i);
                if (isAssignable(issue, participant)) {
                    participant.assignIssue(issue);
                    lastParticipant = i;
                    break;
                }
                if (lastParticipant == i) {
                    break;
                }
            }
        }

        Map<Participant, List<Issue>> aggregatedByAssignee = issues.stream().filter(issue -> issue.getAssignee() != null)
                .collect(Collectors.groupingBy(Issue::getAssignee));
        aggregatedByAssignee.forEach((participant, assignedIssues) -> mailer.send(new Mail()
                .setSubject(EMAIL_SUBJECT)
                .setText(createEmailText(participant.getUsername(), assignedIssues))
                .setTo(List.of(participant.getUsername()))));
    }

    private boolean isAssignable(Issue issue, Participant participant) {
        Tuple2<Integer, Set<String>> issuesComponentsKey = participant.getProjectComponents(issue.getProject());
        return issuesComponentsKey.getItem1() != 0 &&
                issuesComponentsKey.getItem2().containsAll(issue.getComponents());
    }

    public static String createEmailText(String email, List<Issue> issues) {
        Matcher matcher = USERNAME_FROM_EMAIL.matcher(email);
        String username = matcher.find() ? matcher.group(1) : "Somebody";
        return EMAIL_BODY.formatted(username,
                issues.stream().map(issue -> issue.getUri().toString()).collect(Collectors.joining("\n")));
    }
}
