package tr.com.softwaretesting;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

import java.util.Base64;

public class TestCaseGeneration {

    static {
        Unirest.config()
                .connectTimeout(20000); // 20 seconds timeout
    }

    private static final String JIRA_URL = "https://yourjiradomain.com";
    private static final String JIRA_USERNAME = "yourjirausername";
    private static final String JIRA_API_TOKEN = "yourjiraapikey";
    private static final String FIX_VERSION_VALUE = "1.0"; //test issue required field example

    public static void GPTIntegration() {

        Unirest.config()
                .connectTimeout(30000); // 30 second timeout

        String jiraBaseUrl = JIRA_URL;
        String jiraUserEmail = JIRA_USERNAME;
        String jiraApiToken = JIRA_API_TOKEN;
        String jqlQuery = "jql that lists the issues you want to write test cases for";

        // get json format from Jira Api
        String jiraResponse = getJiraData(jiraBaseUrl, jiraUserEmail, jiraApiToken, jqlQuery);

        if (jiraResponse != null) {
            // Process JSON data and send each issue's description field to GPT
            processIssuesAndCreateTestCases(jiraBaseUrl, jiraUserEmail, jiraApiToken, jiraResponse);
        }
    }
    public static void processIssuesAndCreateTestCases(String baseUrl, String email, String apiToken, String jiraResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jiraResponse);
            JSONArray issues = jsonObject.getJSONArray("issues");

            for (int i = 0; i < issues.length(); i++) {
                JSONObject issue = issues.getJSONObject(i);
                String key = issue.getString("key");
                String description = issue.getJSONObject("fields").optString("description", "");
                String issueType = issue.getJSONObject("fields").getJSONObject("issuetype").getString("name");
                String projectKey = key.split("-")[0];

                // Create test cases only for issues with appropriate work types
                if (issueType.equals("Story") || issueType.equals("Improvement") || issueType.equals("Change")) {
                    // Send description field to GPT API and get test case
                    JSONObject testCaseDetails = getTestCaseFromGPT(description);

                    if (testCaseDetails != null) {
                        String testCaseSummary = testCaseDetails.getString("summary");
                        String testCaseDescription = testCaseDetails.getString("description");
                        JSONArray testSteps = testCaseDetails.getJSONArray("steps");

                        System.out.println(testSteps);

                        // Create a new test issue and link it related issue for traceability
                        String testIssueKey = createTestIssue(baseUrl, email, apiToken, testCaseSummary, testCaseDescription, projectKey);
                        if (testIssueKey != null) {
                            linkIssue(baseUrl, email, apiToken, key, testIssueKey);
                             addTestSteps(testIssueKey, testSteps);
                        }
                    }
                } else {
                    System.out.println("Skipping issue " + key + " with type " + issueType);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static JSONObject getTestCaseFromGPT(String description) {
        String apiKey = "your api key";

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gpt-4o");

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        JSONObject responseFormat = new JSONObject();
        message.put("role", "user");
        message.put("assistant_id","your assistant id");
        message.put("content", "Aşağıdaki iş kurallarına göre XRAY formatına uygun summary, description ve steps (steps içinde action, data, result değişkenleri olsun) alanlarından oluşan json formatında tüm alanları Türkçe bir test case oluşturabilir misin? " + description); //You can change the content area according to the question-taking style of the custom gpt you are training

        messages.put(message);
        responseFormat.put("type", "json_object");

        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 1000);
        requestBody.put("response_format",responseFormat);

        try {
            HttpResponse<JsonNode> response = Unirest.post("https://api.openai.com/v1/chat/completions")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(requestBody)
                    .asJson();
            System.out.println(response.getBody().getObject());

            if (response.getStatus() == 200) {
                JSONObject responseObject = response.getBody().getObject();
                JSONArray choicesArray = responseObject.getJSONArray("choices");
                if (choicesArray.length() > 0) {
                    JSONObject firstChoice = choicesArray.getJSONObject(0);
                    JSONObject content = new JSONObject(firstChoice.getJSONObject("message").getString("content"));

                    String summaryPart = content.getString("summary");
                    String descriptionPart = content.getString("description");
                    JSONArray stepsPart = content.getJSONArray("steps");

                    if (summaryPart.length() > 255) {
                        summaryPart = summaryPart.substring(0, 255);
                    }

                    JSONObject result = new JSONObject();
                    result.put("summary", summaryPart + " Testi");
                    result.put("description", descriptionPart);
                    result.put("steps", stepsPart);
                    return result;
                } else {
                    System.err.println("No choices found in the response.");
                    return null;
                }
            } else {
                System.err.println("Error: " + response.getStatus() + " " + response.getStatusText());
                System.err.println("Response: " + response.getBody().toString());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static void addTestSteps(String issueKey, JSONArray testSteps) {
        try {
            String auth = JIRA_USERNAME + ":" + JIRA_API_TOKEN;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            for (int i = 0; i < testSteps.length(); i++) {
                JSONObject step = testSteps.getJSONObject(i);
                JSONObject stepObject = new JSONObject();
                stepObject.put("step", step.getString("action"));
                stepObject.put("data", step.optString("data", ""));
                stepObject.put("result", step.optString("result", "Sonuç belirtilmemiş."));
                stepObject.put("attachments", new JSONArray());

                JSONObject requestBody = new JSONObject();
                requestBody.put("step", stepObject.getString("step"));
                requestBody.put("data", stepObject.getString("data"));
                requestBody.put("result", stepObject.getString("result"));
                requestBody.put("attachments", stepObject.getJSONArray("attachments"));

                System.out.println(requestBody);
                System.out.println(issueKey);

                HttpResponse<JsonNode> response = Unirest.post(JIRA_URL + "/api/v2/issue/" + issueKey + "/teststep")
                        .header("Authorization", "Basic " + encodedAuth)
                        .header("Content-Type", "application/json")
                        .body(requestBody)
                        .asJson();

                if (response.getStatus() == 200 || response.getStatus() == 201) {
                    System.out.println("Test step added successfully.");
                } else {
                    System.err.println("Error adding test step: " + response.getStatus() + " " + response.getStatusText());
                    if (response.getBody() != null) {
                        System.err.println("Response: " + response.getBody().toString());
                    } else {
                        System.err.println("Response body is null.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String createTestIssue(String baseUrl, String email, String apiToken, String summary, String description, String projectKey) {
        try {
            String auth = email + ":" + apiToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            JSONObject requestBody = new JSONObject();
            JSONObject fields = new JSONObject();
            fields.put("summary", summary);
            fields.put("description", description);
            fields.put("issuetype", new JSONObject().put("name", "Test"));
            fields.put("project", new JSONObject().put("key", projectKey));
            fields.put("fixVersions", new JSONArray().put(new JSONObject().put("name", FIX_VERSION_VALUE)));
            fields.put("labels", new JSONArray().put("CreatedByChatGPT")); // Add label to distinguish from manual test cases

            requestBody.put("fields", fields);

            HttpResponse<JsonNode> response = Unirest.post(baseUrl + "/rest/api/2/issue")
                    .header("Authorization", "Basic " + encodedAuth)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .asJson();

            if (response.getStatus() == 201) {
                String testIssueKey = response.getBody().getObject().getString("key");
                System.out.println("Test issue created: " + testIssueKey);
                return testIssueKey;
            } else {
                System.err.println("Error creating test issue: " + response.getStatus() + " " + response.getStatusText());
                System.err.println("Response: " + response.getBody().toString());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void linkIssue(String baseUrl, String email, String apiToken, String issueKey1, String issueKey2) {
        try {
            String auth = email + ":" + apiToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            JSONObject requestBody = new JSONObject();
            requestBody.put("type", new JSONObject().put("id", "10006"));
            requestBody.put("outwardIssue", new JSONObject().put("key", issueKey1));
            requestBody.put("inwardIssue", new JSONObject().put("key", issueKey2));

            HttpResponse<JsonNode> response = Unirest.post(baseUrl + "/rest/api/2/issueLink")
                    .header("Authorization", "Basic " + encodedAuth)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .asJson();

            if (response.getStatus() == 201) {
                System.out.println("Issues linked successfully.");
            } else {
                System.err.println("Error linking issues: " + response.getStatus() + " " + response.getStatusText());
                System.err.println("Response: " + response.getBody().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getJiraData(String baseUrl, String email, String apiToken, String jql) {
        try {
            String auth = email + ":" + apiToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            HttpResponse<JsonNode> response = Unirest.get(baseUrl + "/rest/api/2/search")
                    .header("Authorization", "Basic " + encodedAuth)
                    .queryString("jql", jql)
                    .asJson();

            if (response.getStatus() == 200) {
                return response.getBody().toString();
            } else {
                System.err.println("Error: " + response.getStatus() + " " + response.getStatusText());
                System.err.println("Response: " + response.getBody().toString());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
