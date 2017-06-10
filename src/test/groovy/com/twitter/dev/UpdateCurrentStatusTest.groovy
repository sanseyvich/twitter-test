package com.twitter.dev

import com.twitter.dev.rest.TwitterClient
import com.twitter.dev.utils.Utils
import org.apache.commons.lang.RandomStringUtils
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by sanseyvich on 5/21/17.
 */
class UpdateCurrentStatusTest extends Specification {
    @Shared private TwitterClient twitterClient
    @Shared private config

    def setupSpec() {
        twitterClient = new TwitterClient()
        config = Utils.getConfig()
    }

    //positive cases
    def "user-authenticated consumer can add new status and receive response in JSON format"() {
        when: 'consumer authenticated by user post a new tweet'
        String tweetText = RandomStringUtils.randomAlphanumeric(10)
        def response = twitterClient.statusUpdate(tweetText, AuthorizationType.SINGLE_USER)

        then: 'status is updated and tweet is successfully posted'
        assert response.status == 200
        assert response.data.text == tweetText
        assert response.contentType == 'application/json'

        cleanup: 'cleanup created test tweet'
        twitterClient.statusDestroy(response.data.id)
    }

    //main parameters verifications - positive
    def "'in_reply_to_status_id' parameter creates reply to existent status"() {
        given: 'already created tweet by current user'
        String user_name = config.user.name
        String originalTweet = RandomStringUtils.randomAlphanumeric(10)
        String replyText = "it's a test reply to ${originalTweet} by ${user_name}"
        def originalTweetResponse = twitterClient.statusUpdate(originalTweet, AuthorizationType.SINGLE_USER)
        def originalTweetID = originalTweetResponse.data.id

        when: 'consumer authenticated by user post a reply to created tweet'
        def replyResponse = twitterClient.statusUpdate([
                'status'               : replyText,
                'in_reply_to_status_id': originalTweetID
        ])

        then: 'replied data is appropriately displayed in Tweet response object and so that posted successfully'
        assert replyResponse.status == 200
        assert replyResponse.data.in_reply_to_screen_name == user_name
        assert replyResponse.data.in_reply_to_status_id == originalTweetResponse.data.id
        assert replyResponse.data.in_reply_to_status_id_str == originalTweetResponse.data.id_str
        assert replyResponse.data.in_reply_to_user_id == originalTweetResponse.data.user.id
        assert replyResponse.data.in_reply_to_user_id_str == originalTweetResponse.data.user.id_str
        assert replyResponse.data.text == replyText

        cleanup: 'cleanup created test tweet'
        twitterClient.statusDestroy(originalTweetResponse.data.id)
        twitterClient.statusDestroy(replyResponse.data.id)
    }

    def "'possibly_sensitive' parameter value appears in a response"() {
        given: 'already created tweet by current user and link to these'
        def originalTweetResponse = twitterClient.statusUpdate(RandomStringUtils.randomAlphanumeric(10))
        String sensitiveURI = "https://twitter.com/${config.user.name}/status/${originalTweetResponse.data.id}"

        when: 'consumer authenticated by user post a status that contains a link with possibly_sensitive = true'
        def response = twitterClient.statusUpdate([
                'status'            : sensitiveURI,
                'possibly_sensitive': true
        ])

        then: 'possibly_sensitive entity surfaces in response and shows original value'
        assert response.status == 200
        assert response.data.possibly_sensitive == true

        cleanup: 'cleanup created test tweet'
        twitterClient.statusDestroy(originalTweetResponse.data.id)
        twitterClient.statusDestroy(response.data.id)
    }

    def "'trim_user' parameter trims a user object to include only the status authors numerical ID"() {
        when: 'consumer authenticated by user post a status with including "trim_user=true" parameter'
        def response = twitterClient.statusUpdate([
                'status'   : RandomStringUtils.randomAlphanumeric(10),
                'trim_user': true
        ])

        then: '"user" entity within response has only one "id" object included with numerical value'
        assert response.status == 200
        assert response.data.user.id instanceof Number
        assert response.data.user.size() == 1

        cleanup: 'cleanup created test tweet'
        twitterClient.statusDestroy(response.data.id)
    }

    //negative cases
    def "consumer with application-only authentication can't add new status"() {
        when: 'consumer authenticated with application-only post a new tweet'
        String tweetText = RandomStringUtils.randomAlphanumeric(10)
        def response = twitterClient.statusUpdate(tweetText, AuthorizationType.APPLICATION_ONLY)

        then: 'status is not updated and error occurred'
        assert response.status == 403
        assert response.data.errors.message.get(0) == 'Your credentials do not allow access to this resource.'
        assert response.data.errors.code.get(0) == 220
    }

    def "error while tweeting without status parameter"() {
        when: 'user-authenticated consumer send a request to post a new tweet without status'
        def response = twitterClient.statusUpdate([:])

        then: 'status is not posted and error occurred'
        assert response.status == 403
        assert response.data.errors.message.get(0) == 'Missing required parameter: status.'
        assert response.data.errors.code.get(0) == 170
    }

    def "error is returned while sending duplicated status"() {
        when: 'consumer authenticated by user post a two similar tweets one by one'
        String tweetText = RandomStringUtils.randomAlphanumeric(10)
        def responseFirst = twitterClient.statusUpdate(tweetText)
        def responseSecond = twitterClient.statusUpdate(tweetText)

        then: 'first tweet is posted successfully and second duplicate causes error'
        assert responseFirst.status == 200
        assert responseSecond.status == 403
        assert responseSecond.data.errors.message.get(0) == 'Status is a duplicate.'
        assert responseSecond.data.errors.code.get(0) == 187

        cleanup: 'cleanup created test tweet'
        twitterClient.statusDestroy(responseFirst.data.id)
    }

    //TODO 'media_ids' verification
}
