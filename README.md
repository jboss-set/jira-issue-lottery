# jira-issue-lottery

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Set up the app

### Fork the Repository
Fork this repository to your GitHub account.

### Configure JIRA Lottery
1. Write a valid `jira-issue-lottery.yml` config in the `.github` directory in your forked repository.
    - **Valid Config Example:**
      ```yaml
      delay: P14D
      participants:
        - email: Tadpole@thehuginn.com
          projects:
            - project: WFLY
              components: [Logging]
              maxIssues: 5
            - project: ELY
              components: [HTTP, Core]
              maxIssues: 3
      ```
   `delay`::
   The duration of the lottery cycle before participants are selected again.
   + Duration as a String in ISO-8601 format, mandatory, no default.
   
   `participants`::
   A list of participants with their preferences for participating in the lottery.
   + List of Participants, mandatory, no default.
   
   `email`::
   The email address of the participant, where the notification is sent.
   + String, mandatory, no default.
   
   `projects`::
   The list of JIRA projects the participant is interested in, each with specific components and maximum allowed issues.
   + List of Projects, mandatory, no default.
   
   `project`::
   The name of the JIRA project.
   + String, mandatory, no default.
   
   `components`::
   The specific components within the project that the participant is interested in.
   + List of Strings, mandatory, no default.
   
   `maxIssues`::
   The maximum number of issues the participant is willing to take for the specified project and components.
   + Integer, mandatory, no default.



2. Commit and push the changes to your forked repository.


### Create .env File
1. Create a `.env` file in the main directory of your app.
2. Add the value of `%DEV_JIRA_ISSUE_LOTTERY_CONFIG_FILE_REPO` in the `.env` file to point to a public repository with a valid config file according to first step in [Configuration section](#configure-jira-lottery) such as `https://github.com/jboss-set/jira-issue-lottery`.
      ```env
   %DEV_JIRA_ISSUE_LOTTERY_CONFIG_FILE_REPO=<repository url>
> **NOTE** This will override the default value of  specified in `application.properties` file. If you override the value for all modes (dev, test, prod, when you don't prepend a mode, such as `%dev`), then one of the tests will fail, as it expects the original value.
3. Create a Jira Personal Access Token ([Guide on how to create the token](https://confluence.atlassian.com/enterprise/using-personal-access-tokens-1026032365.html))
4. Add the following variable to the `.env` file:
   ```env
   JIRA_ISSUE_LOTTERY_ACCESS_TOKEN=<jira token>
   QUARKUS_MAILER_FROM=<email address>
   QUARKUS_MAILER_USERNAME=<email address>
   QUARKUS_MAILER_PASSWORD=<email password>

**QUARKUS_MAILER_PASSWORD**
value of **<email_password>** is password to the email address corresponding to **QUARKUS_MAILER_USERNAME**. **Note** You probably want to generate it using _Gmail_ > _Settings_ > _Security_ > _2-Step Verification_ > _App passwords_

***Default email service***
is Gmail. To change this behavior or to override predefined parameters in _applications.properties_ file please refer to [Mailer Extension Documentation](https://quarkus.io/guides/mailer-reference#popular)
   
## Production deployment
1. Log into your Openshift
```shell
oc login ...
```
2. Create secret
```shell
oc create secret generic jira-lottery
--from-literal=JIRA_ISSUE_LOTTERY_ACCESS_TOKEN={TBD}
--from-literal=QUARKUS_MAILER_FROM={TBD}
--from-literal=QUARKUS_MAILER_USERNAME={TBD}
--from-literal=QUARKUS_MAILER_PASSWORD={TBD}
```
3. Build and deploy the application
> **_NOTE:_**  You can use script `deploy.sh` for convenience.
```shell
./mvnw clean package -Dquarkus.openshift.deploy=true
```