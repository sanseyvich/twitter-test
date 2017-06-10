package com.twitter.dev

import com.twitter.dev.rest.TwitterClient
import com.twitter.dev.utils.Utils
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by sanseyvich on 6/8/17.
 */
class GetStatusesUserTimelineTest extends Specification {
    @Shared private TwitterClient twitterClient
    @Shared private def config
    @Shared private def postedTweetResponse
    @Shared private def postedReplyResponse
    @Shared private def postedRetweetResponse
    @Shared private Long idToRetweet
    @Shared private String screenName
    @Shared private Long userId
    @Shared private String tweetText
    @Shared private String replyText

    def setupSpec() {
        twitterClient = new TwitterClient()
        config = Utils.getConfig()
        idToRetweet = config.data.id_to_retweet
        tweetText = 'Tweet for test verifications'
        replyText = 'Reply for test verifications'

        postedTweetResponse = twitterClient.statusUpdate([
                'status': tweetText
        ])
        assert postedTweetResponse.status == 200

        postedReplyResponse = twitterClient.statusUpdate([
                'status'               : replyText,
                'in_reply_to_status_id': postedTweetResponse.data.id
        ])
        assert postedReplyResponse.status == 200

        postedRetweetResponse = twitterClient.statusRetweet(idToRetweet)
        assert postedRetweetResponse.status == 200

        //TODO maybe not really good structure of setup - next initialization depends on requests higher - think and redo
        screenName = postedTweetResponse.data.user.screen_name
        userId = postedTweetResponse.data.user.id
    }

    //positive cases
    def "tweets and replies appear in response with appropriate text"() {
        when: 'consumer get tweets by user_id'
        def response = twitterClient.getStatusesUserTimeline([
                user_id: userId])

        then: 'recent tweets and replies ID\'s are in response and text of each listed accordingly without duplications'
        def tweet = response.data.findAll {twt -> twt.get('id') == postedTweetResponse.data.id}
        def reply = response.data.findAll {twt -> twt.get('id') == postedReplyResponse.data.id}

        assert response.status == 200
        assert tweet.size == 1
        assert tweet.text.get(0) == tweetText
        assert reply.size == 1
        assert reply.text.get(0) == replyText
    }

    def "native retweets are included in response"() {
        when: 'consumer with application-only authentication get tweets by user_id'
        def response = twitterClient.getStatusesUserTimeline([user_id: userId])

        then: 're-tweet are included in response once'
        def reTweet = response.data.findAll { rtwt ->
            rtwt.get('id') == postedRetweetResponse.data.id
        }
        assert response.status == 200
        assert reTweet.size == 1
        assert reTweet.retweeted_status.id.get(0) == idToRetweet
    }

    //main parameters verifications - positive
    def "'screen_name' parameter limits response to only tweets created by user with specified screen name"() {
        when: 'consumer with application-only authentication get tweets by screen_name'
        //String screenName = postedTweetResponse.data.user.screen_name
        def response = twitterClient.getStatusesUserTimeline([
                screen_name: screenName],
                AuthorizationType.APPLICATION_ONLY)

        then: 'only tweets with specified screen_name are appeared in response'
        assert response.status == 200
        response.data.user.each { ue -> assert ue.get('screen_name') == screenName }
    }

    def "'user_id' parameter limits response to only tweets created by user with specified user ID"() {
        when: 'consumer with application-only authentication get tweets by user_id'
        def response = twitterClient.getStatusesUserTimeline([user_id: userId])

        then: 'only tweets with specified user_id are appeared in response'
        assert response.status == 200
        response.data.user.each { ue -> assert ue.get('id') == userId }
    }

    def "'count' parameter limits tweets amount in response to specified"() {
        when: 'consumer with single user authentication get tweets with "count" parameter specified'
        int count = 1
        def response = twitterClient.getStatusesUserTimeline([count: count], AuthorizationType.SINGLE_USER)

        then: 'exact specified amount of tweets are returned'
        assert response.status == 200
        assert response.data.size == count
    }

    def "'exclude_replies' parameter can limit reply data to not include tweet's replies"() {
        when: 'consumer with single user authentication get tweets with "exclude_replies=true" parameter specified'
        def response = twitterClient.getStatusesUserTimeline([exclude_replies: true], AuthorizationType.SINGLE_USER)

        then: 'replies are not listed in response'
        assert response.status == 200
        assert response.data.findAll {rpl -> rpl.get('id') == postedReplyResponse.data.id}.size() == 0
    }

    def "'include_rts' parameter can limit reply data to not include retweets"() {
        when: 'consumer with single user authentication get tweets with "include_rts=false" parameter specified'
        def response = twitterClient.getStatusesUserTimeline([include_rts: false], AuthorizationType.SINGLE_USER)

        then: 'exact specified amount of tweets are returned'
        assert response.status == 200
        assert response.data.findAll {rtwt -> rtwt.get('id') == postedRetweetResponse.data.id}.size() == 0
    }


    //negative cases
    def "error occurred while consumer is not authorized"() {
        when: 'consumer with NONE authentication trying to get tweets by screen_name'
        def response = twitterClient.getStatusesUserTimeline([
                screen_name: screenName],
                AuthorizationType.NONE)

        then: 'error occurred and data about statuses is not returned'
        assert response.status == 400
        assert response.data.size() == 1
        assert response.data.errors.message.get(0) == 'Bad Authentication data.'
        assert response.data.errors.code.get(0) == 215
    }

    def cleanupSpec() {
        twitterClient.statusDestroy(postedTweetResponse.data.id)
        twitterClient.statusDestroy(postedReplyResponse.data.id)
        twitterClient.statusUnretweet(postedRetweetResponse.data.id)
    }


}
