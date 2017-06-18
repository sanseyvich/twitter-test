# twitter-test

-------------------
General description
-------------------
Twitter-test is a framework for automated verification of twitter REST API 
(https://dev.twitter.com/overview/api).
Implementation based on Oauth 1.0a standart with using application-only 
and application authenticated by user authentication types. 
API tests are implemented using Groovy syntax and Spock framework on top of
java.
After each maven test execution html based spock report is generated (output dir
is specified in pom.xml file).
All created via API test tweets, replies and retweets would be cleaned up 
after execution.

-----------------
How to run tests
-----------------
To be able to proceed with test executions followed steps should be done:

1. Create test twitter account on https://twitter.com.
2. Create application based on created account details on https://apps.twitter.com/. 
3. Sign in application on https://apps.twitter.com/ to observe consumer 
and access tokens with secrets.
4. Using https://twitter.com observe any existent tweet ID, so retweet feature
could be tested.
5. Fill configuration.groovy file with appropriate details (obtained during execution
of actions described higher).
6. Run mvn clean test from within project home folder.
