package com.twitter.dev.rest

import com.twitter.dev.AuthorizationType
import com.twitter.dev.utils.Utils
import groovy.util.logging.Log4j2
import groovyx.net.http.RESTClient

/**
 * Created by sanseyvich on 5/23/17.
 *
 */
@Log4j2
class TwitterClient {
    private RESTClient restClientNotAuthorized
    private RESTClient restClientAppOnlyAuthorized
    private RESTClient restClientSingleUserAuthorized
    private def host
    private def statuses_user_timeline_URL
    private def statuses_update_URL
    private def statuses_destroy_URL
    private def statuses_retweet_URL
    private def statuses_unretweet_URL

    TwitterClient(def host = 'https://api.twitter.com',
                  def statuses_user_timeline_URL = '/1.1/statuses/user_timeline.json',
                  def statuses_update_URL = '/1.1/statuses/update.json',
                  def statuses_destroy_URL = '/1.1/statuses/destroy/',
                  def statuses_retweet_URL = '/1.1/statuses/retweet/',
                  def statuses_unretweet_URL = '/1.1/statuses/unretweet/') {
        this.host = host
        this.statuses_user_timeline_URL = statuses_user_timeline_URL
        this.statuses_update_URL = statuses_update_URL
        this.statuses_destroy_URL = statuses_destroy_URL
        this.statuses_retweet_URL = statuses_retweet_URL
        this.statuses_unretweet_URL = statuses_unretweet_URL

        restClientNotAuthorized = new RESTClient(host)
    }

    def getStatusesUserTimeline(AuthorizationType authorizationType = AuthorizationType.APPLICATION_ONLY) {
        log.info "Get user timeline statuses without parameters. Authorization type = ${authorizationType}"

        def response = returnAuthorizedSpecificClient(authorizationType).get([
                path: statuses_user_timeline_URL])

        return response
    }

    //params are accepted as String, "?" is not required
    def getStatusesUserTimeline(String queryString,
                                AuthorizationType authorizationType = AuthorizationType.APPLICATION_ONLY) {
        log.info "Get user timeline statuses. Authorization type = ${authorizationType}"

        def response = returnAuthorizedSpecificClient(authorizationType).get([
                path       : statuses_user_timeline_URL,
                queryString: queryString])

        return response
    }

    def getStatusesUserTimeline(Map<String, ? extends Object> params,
                                AuthorizationType authorizationType = AuthorizationType.APPLICATION_ONLY) {
        log.info "Get user timeline statuses. Authorization type = ${authorizationType}"

        def response = returnAuthorizedSpecificClient(authorizationType).get([
                path : statuses_user_timeline_URL,
                query: params])

        return response
    }

    def statusUpdate(String status,
                     AuthorizationType authorizationType = AuthorizationType.SINGLE_USER) {
        log.info "Post new status without parameters specified. Authorization type = ${authorizationType}"

        def response = returnAuthorizedSpecificClient(authorizationType).post([
                path : statuses_update_URL,
                query: ['status': status]])

        return response
    }

    def statusUpdate(Map<String, ? extends Object> params,
                     AuthorizationType authorizationType = AuthorizationType.SINGLE_USER) {
        log.info "Post new status. Authorization type = ${authorizationType}"

        def response = returnAuthorizedSpecificClient(authorizationType).post([
                path : statuses_update_URL,
                query: params])

        return response
    }

    def statusRetweet(def id,
                      AuthorizationType authorizationType = AuthorizationType.SINGLE_USER) {
        log.info "Retweet status with ${id} ID using POST method. Authorization type = ${authorizationType}"

        def response = returnAuthorizedSpecificClient(authorizationType).post([
                path: "${statuses_retweet_URL}${id}.json"])

        return response
    }

    def statusUnretweet(def id,
                      AuthorizationType authorizationType = AuthorizationType.SINGLE_USER) {
        log.info "Untweet retweeted status with ${id} ID using POST method. Authorization type = ${authorizationType}"

        def response = returnAuthorizedSpecificClient(authorizationType).post([
                path: "${statuses_unretweet_URL}${id}.json"])

        return response
    }

    def statusDestroy(def id,
                      AuthorizationType authorizationType = AuthorizationType.SINGLE_USER) {
        log.info "Delete status with ${id} ID using POST method. Authorization type = ${authorizationType}"

        def response = returnAuthorizedSpecificClient(authorizationType).post([
                path: "${statuses_destroy_URL}${id}.json"])

        return response
    }

    // initializes restClientAppOnlyAuthorized and restClientSingleUserAuthorized
    // updates RESTClient with Authorization headers and return one
    // based on AuthorizationType specified
    private RESTClient returnAuthorizedSpecificClient(AuthorizationType authorizationType) {
        switch (authorizationType) {
            case AuthorizationType.NONE: return restClientNotAuthorized
            case AuthorizationType.APPLICATION_ONLY:
                if (restClientAppOnlyAuthorized == null) {
                    this.restClientAppOnlyAuthorized = new RESTClient(host)
                    addContentTypeAndFailureHandling(this.restClientAppOnlyAuthorized)
                    Utils.addApplicationOnlyAuthorizationHeaderToRestClient(this.restClientAppOnlyAuthorized)
                }
                return restClientAppOnlyAuthorized
            case AuthorizationType.SINGLE_USER:
                if (restClientSingleUserAuthorized == null) {
                    this.restClientSingleUserAuthorized = new RESTClient(host)
                    addContentTypeAndFailureHandling(this.restClientSingleUserAuthorized)
                    Utils.addSingleUserAuthorizationDetailsToRestClient(this.restClientSingleUserAuthorized)
                }
                return restClientSingleUserAuthorized
        }
    }

    private void addContentTypeAndFailureHandling(RESTClient restClient) {
        restClient.contentType = 'application/json'
        restClient.handler.failure = { resp, data ->
            [status: resp.status, data: data]
        }
        log.info "Setup ${host} rest client with contentType = ${restClient.contentType}"
    }
}
