This code defines a Spring Boot REST controller named GithubController.
This controller has an endpoint, /github/repos, which when accessed with a GET request,
retrieves the list of non-fork repositories for a given GitHub username.
The application uses an OkHttpClient to communicate with the GitHub API and is authenticated
using a personal access token. The code returns the repository details, including the repository name,
owner's login, and branch information.
Additionally, it has error handling for unsupported response formats, such as XML, as well as
for scenarios when a GitHub user is not found or other exceptions occur.