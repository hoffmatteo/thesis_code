import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class LexiconMethod {
    private static final String sentiment_lexicon = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\vader_lexicon.txt";
    private static final String negation_lexicon = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\negations.txt";
    private static Map<String, Double> sentimentDictionary = new HashMap<>();
    private static List<String> negationDictionary = new ArrayList<>();

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    //TODO negation has not --> doesnt make sense
    //TODO negation no is also sentiment word?


    public static void main(String[] args) {
        try {
            createSentimentDictionary();
            createNegationDictionary();
            //System.out.println(Arrays.toString(negationDictionary.stream().toArray()));
            startCrawling(10);


        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void analyse(String tweet) {
        System.out.println(ANSI_GREEN + "Analyzing tweet: " + tweet + ANSI_RESET);
        String[] words = tweet.split(" ");
        Double score = 0.0;
        int index = 0;
        for (String word : words) {
            if (sentimentDictionary.containsKey(word)) {
                int polarity = 1;
                System.out.println("Detected sentiment word " + word + " with polarity " + sentimentDictionary.get(word));
                for (int negationIndex = index - 1; negationIndex >= 0 && index - 2 <= negationIndex; negationIndex--) {
                    if (negationDictionary.contains(words[negationIndex].toLowerCase(Locale.ROOT))) {
                        polarity *= -1;
                        //System.out.println("Detected negation word " + words[negationIndex]);
                    }
                }
                score += polarity * sentimentDictionary.get(word);
                System.out.println("Adding to score: " + polarity * sentimentDictionary.get(word));
            }
            index++;
        }
        System.out.println(score);
    }

    public static void startCrawling(int numTweets) throws URISyntaxException, IOException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String bearerToken = System.getenv("bearertoken");
            System.out.println(bearerToken);

            CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
                            .setCookieSpec(CookieSpecs.STANDARD).build())
                    .build();

            URIBuilder uriBuilder = new URIBuilder("https://api.twitter.com/2/tweets/search/recent");

            ArrayList<NameValuePair> queryParameters;
            queryParameters = new ArrayList<>();
            queryParameters.add(new BasicNameValuePair("query", "nba lang:en"));
            queryParameters.add(new BasicNameValuePair("max_results", Integer.toString(numTweets)));

            uriBuilder.addParameters(queryParameters);


            HttpGet httpGet = new HttpGet(uriBuilder.build());
            httpGet.setHeader("Authorization", String.format("Bearer %s", bearerToken));
            httpGet.setHeader("Content-Type", "application/json");

            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();

            if (null != entity) {
                String searchResponse = EntityUtils.toString(entity, "UTF-8");
                System.out.println(searchResponse);
                TwitterResponse myObject = objectMapper.readValue(searchResponse, TwitterResponse.class);
                for (TwitterData data : myObject.data) {
                    analyse(data.text);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void createSentimentDictionary() throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(sentiment_lexicon))) {
            stream.forEach(line -> {
                String[] components = line.split("\t");
                if (components.length >= 2) {
                    double score = Double.parseDouble(components[1]);
                    sentimentDictionary.put(components[0], score);
                }
            });
        }
    }

    private static void createNegationDictionary() throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(negation_lexicon))) {
            stream.forEach(line -> {
                negationDictionary.add(line.toLowerCase(Locale.ROOT));
            });
        }
    }
}

