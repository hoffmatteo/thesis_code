package com.oth.thesis;

import com.oth.thesis.database.AnalyzedTweet;
import com.oth.thesis.twitter.TwitterCrawler;
import com.oth.thesis.twitter.TwitterData;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static com.oth.thesis.twitter.TwitterCrawler.ANSI_GREEN;
import static com.oth.thesis.twitter.TwitterCrawler.ANSI_RESET;

public class LexiconMethod {
    private static final String sentiment_lexicon = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\vader_lexicon.txt";
    private static final String negation_lexicon = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\negations.txt";
    private static final String intensity_lexicon = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\intensities.txt";
    private static final String emoji_lexicon = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\sentiment_lexicon_twitter.csv";
    private static final Map<String, Double> sentimentDictionary = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final List<String> negationDictionary = new ArrayList<>();
    private static final Map<String, Double> intensityDictionary = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final Map<String, Double> emojiDictionary = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static SessionFactory sessionFactory;


    //TODO negation has not --> doesnt make sense
    //TODO negation no is also sentiment word?


    public static void main(String[] args) {
        try {
            startDatabase();
            createSentimentDictionary();
            createNegationList();
            createIntensityList();
            createEmojiDictionary();
            TwitterCrawler crawler = new TwitterCrawler();
            crawler.crawlTweets(50, "ukraine");


            //analyze("Unfortunately, they are not bright \uD83D\uDE06.");
        } catch (IOException ex) {
            ex.printStackTrace();

        }
    }

    public static void analyze(TwitterData tweet) {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        String[] words = tweet.text.split(" ");
        double score = 0.0;
        int index = 0;
        //TODO better
        System.out.println(ANSI_GREEN + "Analyzing tweet: " + tweet + ANSI_RESET);
        for (int i = 0; i < words.length; i++) {
            words[i] = preprocess(words[i]);
        }
        for (String word : words) {
            if (sentimentDictionary.containsKey(word)) {
                if (word.equals("no")) {
                    if (words.length - 1 > index) {
                        if (sentimentDictionary.containsKey(words[index + 1])) {
                            //do not add score as it is a negation
                            index++;
                            continue;
                        }
                    }
                }
                int polarity = 1;
                for (int tempIndex = index - 1; tempIndex >= 0 && index - 2 <= tempIndex; tempIndex--) {
                    if (negationDictionary.contains(words[tempIndex])) {
                        polarity *= -1;
                        //System.out.println("Detected negation word " + words[tempIndex]);
                    } else if (intensityDictionary.containsKey(words[tempIndex])) {
                        polarity *= intensityDictionary.get(words[tempIndex]);
                    }
                }
                score += polarity * sentimentDictionary.get(word);
                System.out.println("Detected sentiment word " + word + " with polarity " + polarity * sentimentDictionary.get(word));
            } /*else if (emojiDictionary.containsKey(word)) {
                score += emojiDictionary.get(word);
                //😯
            }
            */
            index++;
        }
        //TDOo
        session.saveOrUpdate(new AnalyzedTweet(tweet.id, tweet.text, score));
        System.out.println(TwitterCrawler.ANSI_BLUE + "Final score: " + score + ANSI_RESET);
        session.getTransaction().commit();
        session.close();

    }

    //TODO emojis
    private static String preprocess(String word) {
        String removedPunctuation = word.replaceAll("\\p{Punct}", "");
        return removedPunctuation.toLowerCase(Locale.ROOT);
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

    private static void createNegationList() throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(negation_lexicon))) {
            stream.forEach(line -> {
                negationDictionary.add(line.toLowerCase(Locale.ROOT));
            });
        }
    }

    private static void createIntensityList() throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(intensity_lexicon))) {
            stream.forEach(line -> {
                String[] components = line.split(",");
                if (components.length == 2) {
                    double score = Double.parseDouble(components[1]);
                    intensityDictionary.put(components[0].toLowerCase(Locale.ROOT), score);
                }
            });
        }
    }

    private static void createEmojiDictionary() throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(emoji_lexicon))) {
            stream.forEach(line -> {
                String[] components = line.split(",");
                if (components.length >= 3) {
                    double score = Double.parseDouble(components[2]);
                    intensityDictionary.put(components[1].toLowerCase(Locale.ROOT), score);
                }
            });
        }
    }

    private static void startDatabase() {
// configures settings from hibernate.cfg.xml
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure().build();
        try {
            sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

