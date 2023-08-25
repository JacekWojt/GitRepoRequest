package pl.jw.gitreporequest;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
class GithubController {

    private final static String BASE_URL = "http://api.github.com";
    private final static OkHttpClient CLIENT = new OkHttpClient();

    @Value("${github.token}")
    private String githubToken;

    @GetMapping("/github/repos")
    public ResponseEntity<Object> getUserRepos(
            @RequestParam String username,
            @RequestHeader("Accept") String acceptHeader) {

        if ("application/xml".equals(acceptHeader)) {
            return new ResponseEntity<>(
                    createErrorResponse(406, "The application/xml format is not supported."),
                    HttpStatus.NOT_ACCEPTABLE
            );
        }

        try {
            List<JSONObject> nonForkRepos = fetchNonForkRepos(username);
            List<JSONObject> results = new ArrayList<>();

            for (JSONObject repo : nonForkRepos) {
                String repoName = repo.getString("name");
                String ownerLogin = repo.getJSONObject("owner").getString("login");
                JSONArray branches = fetchBranches(ownerLogin, repoName);

                JSONObject repoDetails = new JSONObject();
                repoDetails.put("Repository", repoName);
                repoDetails.put("Owner", ownerLogin);
                repoDetails.put("Branches", branches);

                results.add(repoDetails);
            }
            return ResponseEntity.ok(results);

        } catch (UserNotFoundException e) {
            return new ResponseEntity<>(
                    createErrorResponse(404, e.getMessage()),
                    HttpStatus.NOT_FOUND
            );
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(
                    createErrorResponse(500, "Internal server error."),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private List<JSONObject> fetchNonForkRepos(String username) throws UserNotFoundException {
        List<JSONObject> nonForkRepos = new ArrayList<>();

        Request request = new Request.Builder()
                .url(BASE_URL + "/users/" + username + "/repos")
                .header("Accept", "application/json")
                .header("Authorization", "token" + githubToken)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (response.code() == 404) {
                throw new UserNotFoundException("A GitHub user named " + username + " was not found.");
            }

            String responseBody = response.body().string();
            JSONArray repos = new JSONArray(responseBody);
            for (int i = 0; i < repos.length(); i++) {
                JSONObject repo = repos.getJSONObject(i);
                if (!repo.getBoolean("fork")) {
                    nonForkRepos.add(repo);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nonForkRepos;
    }

    private JSONArray fetchBranches(String owner, String repoName) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/repos/" + owner + "/" + repoName + "/branches")
                .header("Accept", "application/json")
                .header("Authorization", "token" + githubToken)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            return new JSONArray(response.body().string());
        }
    }

    private JSONObject createErrorResponse(int statusCode, String message) {
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("status", statusCode);
        errorResponse.put("Message", message);
        return errorResponse;
    }
}
