package com.twitter.dev.utils

import groovyx.net.http.RESTClient
import org.apache.commons.codec.binary.Base64
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.codehaus.groovy.control.ConfigurationException

/**
 * Created by sanseyvich on 5/28/17.
 */
class Utils {
    //Java logging style is user because it seems not possible to create static logger via groovy annotation
    private static final Logger log = LogManager.getLogger(Utils)

    // get ConfigurationObject fro configuration file
    // authorization.CONSUMER_KEY, authorization.CONSUMER_SECRET, authorization.ACCESS_TOKEN,
    // authorization.ACCESS_SECRET, config.user.name, config.data.id_to_retweet - should be specified otherwise
    // exception would be thrown
    static ConfigObject getConfig() throws MissingPropertyException {
        //URL url = new File('src/main/resources/configuration.groovy').toURI().toURL()
        URL url = Utils.class.getClassLoader().getResource('configuration.groovy')
        def config = new ConfigSlurper().parse(url)

        //check if needed properties are not specified
        if (config.authorization.CONSUMER_KEY instanceof ConfigObject ||
                config.authorization.CONSUMER_SECRET instanceof ConfigObject ||
                config.authorization.ACCESS_TOKEN instanceof ConfigObject ||
                config.authorization.ACCESS_SECRET instanceof ConfigObject ||
                config.user.name instanceof ConfigObject ||
                config.data.id_to_retweet instanceof ConfigObject)
        throw new ConfigurationException('authorization.CONSUMER_KEY, ' +
                'authorization.CONSUMER_SECRET, authorization.ACCESS_TOKEN, ' +
                'authorization.ACCESS_SECRET, \n config.user.name, config.data.id_to_retweet ' +
                '- should be specified in configuration file')

        //check if needed properties are not empty/null
        if (config.authorization.CONSUMER_KEY == '' ||
                config.authorization.CONSUMER_SECRET == '' ||
                config.authorization.ACCESS_TOKEN == '' ||
                config.authorization.ACCESS_SECRET == '' ||
                config.user.name == '' ||
                config.data.id_to_retweet == null)
            throw new ConfigurationException('Configuration file should not include empty or null values')

        return config
    }

    // method authenticates given RESTClient via updating AuthConfig object of one
    // single user Oauth authentication is used
    // note that single user Oauth can be used only if CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN and ACCESS_SECRET
    // has been signed by server already and specified in configuration.groovy file
    // updated RESTCLient would be signed for all further requests until setting restClient.getAuth().oauth(null, ..)
    static void addSingleUserAuthorizationDetailsToRestClient(RESTClient restClient) {
        log.info 'Adding single user authorization data'
        ConfigObject config = getConfig()
        String consumer_key = config.authorization.CONSUMER_KEY.toString()
        String consumer_secret = config.authorization.CONSUMER_SECRET.toString()
        String access_token = config.authorization.ACCESS_TOKEN.toString()
        String access_secret = config.authorization.ACCESS_SECRET.toString()

        restClient.auth.oauth(consumer_key, consumer_secret, access_token, access_secret)
    }

    // method authenticates given RESTClient object via adding signed bearer token as Authorization header
    // application-only authentication is used
    // note that application-only Oauth can be used only if CONSUMER_KEY and CONSUMER_SECRET has been signed by server
    // already and specified in configuration.groovy file
    // updated RESTCLient would be signed for all further requests until Authorization header is removed
    static void addApplicationOnlyAuthorizationHeaderToRestClient(RESTClient restClient) {
        log.info 'Adding Application-only authorization header'
        ConfigObject config = getConfig()
        String consumer_key = config.authorization.CONSUMER_KEY.toString()
        String consumer_secret = config.authorization.CONSUMER_SECRET.toString()
        String encodedCredentials = encodeKeys(consumer_key, consumer_secret)
        def response = restClient.post([
                path   : '/oauth2/token',
                headers: ['Authorization': "Basic ${encodedCredentials}"],
                requestContentType : 'application/x-www-form-urlencoded;charset=UTF-8',
                body   : 'grant_type=client_credentials'])
        assert response.status == 200
        assert response.data.token_type == 'bearer'
        restClient.headers['Authorization'] = "Bearer ${response.data.access_token}"
    }

    static void cleanupAuthorizationDetails(RESTClient restClient) {
        log.info 'cleanup "Authorization" header or ${restClient}'
        restClient.auth.oauth(null, null, null, null)
        Map headers = restClient.getHeaders()
        if (headers.get('Authorization') != null) {
            headers.remove('Authorization')
        }
    }

    private static String encodeKeys(String consumerKey, String consumerSecret) {
        try {
            String encodedConsumerKey = URLEncoder.encode(consumerKey, "UTF-8");
            String encodedConsumerSecret = URLEncoder.encode(consumerSecret, "UTF-8");
            String fullKey = encodedConsumerKey + ":" + encodedConsumerSecret;
            byte[] encodedBytes = Base64.encodeBase64(fullKey.getBytes());
            return new String(encodedBytes);
        }
        catch (UnsupportedEncodingException e) {
            return new String();
        }
    }
}



