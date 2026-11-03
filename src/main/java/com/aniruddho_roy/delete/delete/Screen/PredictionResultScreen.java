package com.aniruddho_roy.delete.delete.Screen;

import com.aniruddho_roy.delete.delete.additional.NAVIGATOR;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Locale;

public class PredictionResultScreen extends DashboardBase {

    public PredictionResultScreen(NAVIGATOR navigator, boolean subscribed, JsonNode result) {
        super(navigator, subscribed);
        setCenter(createResultCenter(navigator, result));
    }

    static ScrollPane createResultCenter(NAVIGATOR navigator, JsonNode result) {
        JsonNode features = result.path("features");
        JsonNode analysis = result.path("analysis");

        Label heading = label("Prediction Result", "heading");
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        VBox content = new VBox(18, heading);
        content.setPadding(new Insets(25));
        content.getStyleClass().add("dashboard-content");

        VBox overview = card("Pull Request");
        addText(overview, "", features.path("repository"));
        String number = features.hasNonNull("number") ? "#" + features.path("number").asText() : "";
        String title = features.path("title").asText("");
        if (!number.isBlank() || !title.isBlank()) {
            overview.getChildren().add(label((number + "  " + title).trim(), "heading"));
        }
        addText(overview, "", result.path("pr_url"));
        addText(overview, "State: ", features.path("state"));
        addText(overview, "Author: ", features.path("author"));
        content.getChildren().add(overview);

        FlowPane scores = new FlowPane(15, 15);
        addScore(scores, "Merge Probability", analysis.path("merge_probability"), "");
        addScore(scores, "PR Quality", analysis.path("quality_score"), analysis.path("quality").asText(""));
        if (!scores.getChildren().isEmpty()) {
            content.getChildren().add(scores);
        }

        VBox statistics = card("Change Statistics");
        addText(statistics, "Lines added: ", features.path("additions"));
        addText(statistics, "Lines deleted: ", features.path("deletions"));
        addText(statistics, "Files changed: ", features.path("changed_files"));
        addText(statistics, "Commits: ", features.path("commit_count"));
        addIfPopulated(content, statistics);

        VBox explanation = card("Analysis");
        addText(explanation, "Summary: ", analysis.path("summary"));
        addText(explanation, "Merge reasoning: ", analysis.path("merge_reasoning"));
        addText(explanation, "Confidence: ", analysis.path("confidence"));
        addIfPopulated(content, explanation);

        VBox recommendations = card("Recommendations");
        JsonNode groups = analysis.path("recommendations");
        if (groups.isObject()) {
            groups.fields().forEachRemaining(group -> {
                JsonNode items = group.getValue();
                if (items.isArray() && !items.isEmpty()) {
                    recommendations.getChildren().add(label(group.getKey(), "heading"));
                    addList(recommendations, items);
                }
            });
        } else {
            addList(recommendations, groups);
        }
        addIfPopulated(content, recommendations);

        VBox improvements = card("Potential Improvements");
        addList(improvements, analysis.path("potential_improvements"));
        addIfPopulated(content, improvements);

        VBox limitations = card("Limitations");
        addList(limitations, analysis.path("limitations"));
        addText(limitations, "", result.path("probability_note"));
        addIfPopulated(content, limitations);

        Button newPrediction = new Button("New Prediction");
        newPrediction.getStyleClass().add("primary-button");
        newPrediction.setOnAction(event -> navigator.loadNewPredictionScreen());
        Button history = new Button("View History");
        history.getStyleClass().add("secondary-button");
        history.setOnAction(event -> navigator.loadHistoryScreen());
        content.getChildren().add(new HBox(12, newPrediction, history));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("transparent-scroll-pane");
        return scroll;
    }

    private static void addScore(FlowPane scores, String title, JsonNode value, String description) {
        if (!value.isNumber()) return;
        double score = value.asDouble();
        Label scoreLabel = label(String.format(Locale.US, "%.1f%%", score),
                score >= 70 ? "probability-high" : score >= 50 ? "probability-medium" : "probability-low");
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        ProgressBar progress = new ProgressBar(Math.max(0, Math.min(1, score / 100)));
        progress.setMaxWidth(Double.MAX_VALUE);
        VBox card = new VBox(10, label(title, "muted-text"), scoreLabel, progress);
        if (!description.isBlank()) card.getChildren().add(label(description, "heading"));
        card.setPadding(new Insets(18));
        card.setPrefWidth(240);
        card.getStyleClass().add("stat-card");
        scores.getChildren().add(card);
    }

    private static VBox card(String title) {
        Label heading = label(title, "heading");
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        VBox card = new VBox(10, heading);
        card.setPadding(new Insets(20));
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("surface-card");
        return card;
    }

    private static Label label(String text, String style) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add(style);
        return label;
    }

    private static void addText(VBox box, String prefix, JsonNode value) {
        if (value.isValueNode() && !value.isNull() && !value.asText().isBlank()) {
            box.getChildren().add(label(prefix + value.asText(), "muted-text"));
        }
    }

    private static void addList(VBox box, JsonNode items) {
        if (items.isArray()) {
            for (JsonNode item : items) addText(box, "- ", item);
        } else {
            addText(box, "", items);
        }
    }

    private static void addIfPopulated(VBox content, VBox card) {
        if (card.getChildren().size() > 1) content.getChildren().add(card);
    }
}
