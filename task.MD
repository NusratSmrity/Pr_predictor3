# Necessary API Changes

Based on the current JavaFX frontend and backend at:
R:/BOOKS/L3-S1/CSE 302-Software Development Sessional/project/pr_predictor_backend_v2

Only the necessary changes are listed below. Keep the code small and simple: update the existing classes and reuse Jackson JsonNode. Private admin APIs are reserved for the web app.

## 1. POST /predict — request

Files: Backend/Predictions.java and Screen/NewPredictionScreen.java.

- [x] Send only pullRequestUrl and required modelId (the UUID returned by GET /models).
- [x] Remove predictionType from the request and method arguments. The backend rejects it with HTTP 422.
- [x] Remove the unnecessary prediction-type selector.
- [x] Validate HTTPS GitHub pull-request URLs.

Request:

~~~json
{
  "pullRequestUrl": "https://github.com/owner/repo/pull/12",
  "modelId": "<model UUID>"
}
~~~

## 2. POST /predict — result display

File: Screen/PredictionResultScreen.java.

- [x] Read scores from analysis.merge_probability, analysis.quality_score and analysis.quality.
- [x] Read PR information from features.repository, features.number, features.title and the top-level pr_url.
- [x] Replace old statistics with features.additions, features.deletions, features.changed_files and features.commit_count.
- [x] Replace the old factors/recommendation display with analysis.summary, analysis.merge_reasoning and analysis.recommendations.
- [x] Hide unsupported statistics and metadata instead of showing misleading zeros or unknown values. The immediate response does not add predictionId, model metadata, predictionType, status or creditsCost.

## 3. GET /history — history and recent predictions

Files: Screen/HistoryScreen.java and additional/COMPONETS.java.

- [x] Replace reads of the old repository/pullRequest objects with features.repository, features.number and features.title.
- [x] Handle FAILED records with empty features/analysis and null scores. Use pr_url as a fallback and show the failure status/errorMessage.
- [x] Avoid parsing "-" as a number in the recent-prediction card.
- [x] Keep the existing top-level mergeProbability, qualityScore and qualityLabel reads for history; these fields are still returned.

## 4. GET /plans, /active-plan and /subscription — credits and duration

Files: Backend/Subscription.java, Screen/SubscriptionScreen.java and Screen/ProfileScreen.java.

- [ ] Read credits instead of monthlyCredits and update the small records/accessors and their callers.
- [ ] Read duration in days and the subscription's endedAt.
- [ ] Update plan cards and profile usage displays to use the actual credits and plan period.
- [ ] Replace "credits / month" with "credits per N days" or "credits per plan period".

## 5. GET /dashboard — plan display

File: Screen/UserDashboard.java.

- [ ] Read subscription.plan.credits instead of subscription.plan.monthlyCredits.
- [ ] Use subscription.plan.duration for the plan period.
- [ ] Replace monthly usage labels with "current plan period".
- [ ] Keep the existing usage.predictionsUsed, usage.creditsUsed, usage.creditLimit and usage.creditsRemaining reads; these fields still match.

## Already compatible

No API-contract changes are needed for login, registration, profile, change-password, settings, model listing or POST /subscription.

All Java paths above are relative to src/main/java/com/aniruddho_roy/delete/delete/.
