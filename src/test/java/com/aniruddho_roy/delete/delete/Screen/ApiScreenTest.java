package com.aniruddho_roy.delete.delete.Screen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aniruddho_roy.delete.delete.additional.COMPONETS;
import com.aniruddho_roy.delete.delete.Backend.Subscription.CurrentSubscription;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ApiScreenTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(ready::countDown);
        assertTrue(ready.await(10, TimeUnit.SECONDS));
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    void resultDisplaysNestedScoresAndFeatures() throws Exception {
        onFxThread(() -> {
            var result = new ObjectMapper().readTree("""
                    {"pr_url":"https://github.com/owner/repo/pull/12",
                     "features":{"repository":"owner/repo","number":12,"title":"Fix login",
                                 "additions":40,"deletions":0,"changed_files":5,"commit_count":2},
                     "analysis":{"merge_probability":95,"quality_score":92,"quality":"excellent",
                                 "summary":"Fixes login.","merge_reasoning":"Small tested fix.",
                                 "recommendations":{"excellent":["Tests included"],"bad":[]}}}
                    """);
            List<String> texts = texts(PredictionResultScreen.createResultCenter(null, result));
            assertTrue(texts.containsAll(List.of("owner/repo", "#12  Fix login", "95.0%", "92.0%",
                    "excellent", "Lines added: 40", "Lines deleted: 0", "Files changed: 5",
                    "Commits: 2", "Summary: Fixes login.", "Merge reasoning: Small tested fix.",
                    "- Tests included", "https://github.com/owner/repo/pull/12")));
        });
    }

    @Test
    void missingFieldsStayHiddenButZeroScoresRemainVisible() throws Exception {
        onFxThread(() -> {
            var result = new ObjectMapper().readTree("""
                    {"features":{},"analysis":{"merge_probability":0,"quality_score":null}}
                    """);
            List<String> texts = texts(PredictionResultScreen.createResultCenter(null, result));
            assertTrue(texts.contains("0.0%"));
            for (String absent : List.of("PR Quality", "Change Statistics", "Model", "Prediction Type",
                    "Analysis", "Recommendations", "Unknown", "Lines added: 0")) {
                assertFalse(texts.contains(absent), absent);
            }
        });
    }

    @Test
    void historyAndRecentCardsUseFeaturesAndKeepHistoryScores() throws Exception {
        onFxThread(() -> {
            var item = new ObjectMapper().readTree("""
                    {"status":"COMPLETED","features":{"repository":"owner/repo","number":12,
                     "title":"Fix login"},"mergeProbability":95,"qualityScore":92,
                     "qualityLabel":"excellent","createdAt":"2026-11-01T12:00:00"}
                    """);
            List<String> row = texts(HistoryScreen.createPredictionRow(item));
            List<String> recent = texts(new COMPONETS().createPredictionItem(item));
            assertTrue(row.containsAll(List.of("owner/repo", "#12  Fix login", "95%", "excellent")));
            assertTrue(recent.containsAll(List.of("owner/repo", "#12  Fix login", "95%")));
        });
    }

    @Test
    void failedPredictionsShowUrlAndErrorWithoutParsingMissingScores() throws Exception {
        onFxThread(() -> {
            var item = new ObjectMapper().readTree("""
                    {"status":"FAILED","features":{},"analysis":{},
                     "pr_url":"https://github.com/owner/repo/pull/12",
                     "errorMessage":"Service unavailable","mergeProbability":null,"qualityScore":null}
                    """);
            for (Node card : List.of(HistoryScreen.createPredictionRow(item),
                    new COMPONETS().createPredictionItem(item))) {
                assertTrue(texts(card).containsAll(List.of("https://github.com/owner/repo/pull/12",
                        "FAILED: Service unavailable", "-")));
            }
        });
    }

    @Test
    void profileShowsPlanPeriodAndExpiry() throws Exception {
        onFxThread(() -> {
            var current = new CurrentSubscription("FREE", "Free", "ACTIVE", "2026-11-03",
                    "2026-11-04T12:00:00", 5, 1, 2, 3);
            String content = String.join("\n", texts(ProfileScreen.createCurrentSubscriptionCard(current)));
            assertTrue(content.contains("2 / 5 credits used"));
            assertTrue(content.contains("1 day"));
            assertTrue(content.contains("Ends 2026-11-04 12:00:00 UTC"));
            assertFalse(content.contains("month"));
        });
    }

    @Test
    void dashboardShowsCreditsForTheActualPlanDuration() throws Exception {
        onFxThread(() -> {
            for (int duration : new int[]{1, 30}) {
                var subscription = new ObjectMapper().readTree(
                        "{\"status\":\"ACTIVE\",\"plan\":{\"name\":\"Plan\",\"code\":\"PLAN\","
                        + "\"credits\":5,\"duration\":" + duration + "}}");
                String content = String.join("\n", texts(UserDashboard.createPlanCard(subscription)));
                assertTrue(content.contains("5 credits per " + duration + (duration == 1 ? " day" : " days")));
                assertFalse(content.contains("month"));
            }
        });
    }

    private static List<String> texts(Node node) {
        List<String> result = new ArrayList<>();
        if (node instanceof Labeled labeled) result.add(labeled.getText());
        if (node instanceof ScrollPane scroll) {
            result.addAll(texts(scroll.getContent()));
        } else if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) result.addAll(texts(child));
        }
        return result;
    }

    private static void onFxThread(CheckedAction action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> { action.run(); return null; });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

    private interface CheckedAction {
        void run() throws Exception;
    }
}
