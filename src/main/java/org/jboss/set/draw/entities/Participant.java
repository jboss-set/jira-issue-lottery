package org.jboss.set.draw.entities;

import io.smallrye.mutiny.tuples.Tuple2;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Participant {

    private final String email;
    private Integer assignableIssues;
    private final Map<String, Tuple2<Integer, Set<String>>> projectComponentsMap;

    public Participant(String email, Integer assignableIssues, Map<String, Tuple2<Integer, Set<String>>> projectComponentsMap) {
        this.email = email;
        this.assignableIssues = assignableIssues;
        this.projectComponentsMap = projectComponentsMap;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Participant that = (Participant) o;
        return Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    public void assignIssue(Issue issue) {
        if (assignableIssues != null) {
            assignableIssues--;
        }
        issue.setAssignee(this);
        Tuple2<Integer, Set<String>> entry = projectComponentsMap.get(issue.getProject());
        projectComponentsMap.put(issue.getProject(), Tuple2.of(entry.getItem1() - 1, entry.getItem2()));
    }

    private Tuple2<Integer, Set<String>> getProjectComponents(String project) {
        return projectComponentsMap.getOrDefault(project, Tuple2.of(0, Collections.emptySet()));
    }

    public boolean isAssignable(Issue issue) {
        Tuple2<Integer, Set<String>> issuesComponentsKey = getProjectComponents(issue.getProject());
        List<String> components = issue.getComponents();
        boolean betweenComponents = issuesComponentsKey.getItem2() == null ||
                !issuesComponentsKey.getItem2().stream().filter(components::contains).toList().isEmpty();
        boolean canAssignIssue = assignableIssues == null || assignableIssues > 0;
        return issuesComponentsKey.getItem1() != 0 && betweenComponents && canAssignIssue;
    }
}
