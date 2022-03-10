package com.oth.thesis.twitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oth.thesis.database.AnalyzedTweet;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class TwitterCrawler {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final int maxTweetBatch = 100; //set by twitter
    public static final int maxNumTweets = 2000; //set by me
    public static final int minTweetBatch = 10; //set by twitter
    private final String bearerToken;
    private CloseableHttpClient httpClient;
    private final String englishTweets = " lang:en";
    private final String noRetweets = " -is:retweet";

    public TwitterCrawler() {
        bearerToken = System.getenv("bearertoken");
        httpClient = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

    }

    public void crawlTweets(int amount, String query, SessionFactory sessionFactory) {
        if (amount > maxNumTweets) {
            return;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> results = new ArrayList<>();
        int numTweets = 0;
        TwitterResponse twitterReponse = null;


        while (numTweets < amount) {
            try {
                URIBuilder uriBuilder = null;
                uriBuilder = new URIBuilder("https://api.twitter.com/2/tweets/search/recent");

                ArrayList<NameValuePair> queryParameters;
                queryParameters = new ArrayList<>();
                queryParameters.add(new BasicNameValuePair("query", query + englishTweets + noRetweets));
                if (amount - numTweets >= maxTweetBatch) {
                    //request the maximum amount as often as needed
                    queryParameters.add(new BasicNameValuePair("max_results", Integer.toString(maxTweetBatch)));
                } else {
                    queryParameters.add(new BasicNameValuePair("max_results", Integer.toString(Math.max((amount - numTweets), minTweetBatch))));
                }
                if (numTweets != 0) {
                    if (twitterReponse.meta.next_token != null) {
                        //next token ensures that no duplicate tweets are sent by twitter
                        queryParameters.add(new BasicNameValuePair("next_token", twitterReponse.meta.next_token));
                    } else {
                        System.out.println("No more tweets available!");
                        break;
                    }
                }

                uriBuilder.addParameters(queryParameters);

                HttpGet httpGet = new HttpGet(uriBuilder.build());
                httpGet.setHeader("Authorization", String.format("Bearer %s", bearerToken));
                httpGet.setHeader("Content-Type", "application/json");

                CloseableHttpResponse response = httpClient.execute(httpGet);
                HttpEntity entity = response.getEntity();

                if (null != entity) {
                    String searchResponse = EntityUtils.toString(entity, "UTF-8");
                    System.out.println(searchResponse);
                    twitterReponse = objectMapper.readValue(searchResponse, TwitterResponse.class);
                    for (TwitterData data : twitterReponse.data) {
                        //LexiconMethod.analyze(data);
                        Session session = sessionFactory.openSession();
                        session.beginTransaction();
                        session.saveOrUpdate(new AnalyzedTweet(data.id, data.text));
                        session.getTransaction().commit();
                        session.close();
                        numTweets++;
                    }
                }
            } catch (IOException | URISyntaxException e) {

            }
        }

    }


}
