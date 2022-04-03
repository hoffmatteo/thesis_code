package com.oth.thesis;

import com.oth.thesis.database.AnalyzedTweet;
import com.oth.thesis.database.TrainingTweet;
import com.oth.thesis.twitter.TwitterCrawler;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.oth.thesis.twitter.TwitterCrawler.ANSI_GREEN;
import static com.oth.thesis.twitter.TwitterCrawler.ANSI_RESET;

public class LexiconMethod {
    private static final String sentiment_lexicon = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\lexicons\\vader_lexicon.txt";
    private static final String negation_lexicon = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\lexicons\\negations.txt";
    private static final String intensity_lexicon = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\lexicons\\intensities.txt";
    private static final String emoji_lexicon = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\lexicons\\sentiment_lexicon_twitter.csv";
    private final Map<String, Double> sentimentDictionary = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final List<String> negationDictionary = new ArrayList<>();
    private final Map<String, Double> intensityDictionary = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, Double> emojiDictionary = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private SessionFactory sessionFactory;


    //TODO negation has not --> doesnt make sense
    //TODO negation no is also sentiment word?


    public static void main(String[] args) {

    }

    public LexiconMethod(SessionFactory factory) {
        sessionFactory = factory;
        try {
            createSentimentDictionary();
            createNegationList();
            createIntensityList();
            createEmojiDictionary();
            //TwitterCrawler crawler = new TwitterCrawler();
            //ZelenskyyUA, Lebron, Bitcoin, Disney, Scholz, Microsoft
            //crawler.crawlTweets(2000, "Microsoft", sessionFactory);
            //analyzeTweets();
            //analyzeTweet("Very funny \uD83D\uDE02!");
            //TrainingData.create(sessionFactory);
            evaluate();
        } catch (IOException ex) {
            ex.printStackTrace();

        }
    }

    public double analyzeTweet(String tweet) {
        String[] words = tweet.split(" ");
        double score = 0.0;
        int index = 0;
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

            } else if (emojiDictionary.containsKey(word)) {
                score += emojiDictionary.get(word);
                System.out.println("Detected emoji " + word + " with polarity " + emojiDictionary.get(word));
                //😯
            }

            index++;
        }
        System.out.println(TwitterCrawler.ANSI_BLUE + "Final score: " + score + ANSI_RESET);
        return score;


    }

    public void analyzeTweets() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        List<AnalyzedTweet> result = session.createQuery("from AnalyzedTweet ", AnalyzedTweet.class).list();
        result.forEach(tweet -> {

            double score = analyzeTweet(tweet.getText());
            tweet.setLexicon_score(score);
            session.saveOrUpdate(tweet);
        });
        session.getTransaction().commit();
        session.close();


    }

    public void evaluate() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        List<TrainingTweet> tweets = session.createQuery("from TrainingTweet ", TrainingTweet.class).list();
        AtomicInteger falseTweets = new AtomicInteger(0);
        AtomicInteger correctTweets = new AtomicInteger(0);

        tweets.forEach(tweet -> {
            double lexiconScore = analyzeTweet(tweet.getText());
            double correctScore = tweet.getScore();
            if (lexiconScore < -0.5 || lexiconScore > 0.5) {
                if (sameSign(lexiconScore, correctScore)) {
                    correctTweets.incrementAndGet();
                } else {
                    falseTweets.incrementAndGet();
                }
            } else if (correctScore == 0) {
                correctTweets.incrementAndGet();
            } else {
                falseTweets.incrementAndGet();
            }
        });

        /*
        Correctly Classified Instances        2883               60.9901 %
Incorrectly Classified Instances      1844               39.0099 %
Kappa statistic                          0.2073
Mean absolute error                      0.4126
Root mean squared error                  0.5241
Relative absolute error                 82.1423 %
Root relative squared error            104.3431 %
Total Number of Instances             4727
         */
        System.out.println("Lexicon Method Evaluation: ");
        int totalInstances = correctTweets.get() + falseTweets.get();
        double correctPercentage = (double) correctTweets.get() / totalInstances * 100;
        double falsePercentage = (double) falseTweets.get() / totalInstances * 100;

        System.out.println("Correctly classified instances:\t " + correctTweets.get() + "\t " + correctPercentage + "%");
        System.out.println("Incorrectly classified instances:\t " + falseTweets.get() + "\t " + falsePercentage + "%");
        System.out.println("Total number instances:\t " + totalInstances);


    }

    private boolean sameSign(double num1, double num2) {
        return num1 >= 0 && num2 >= 0 || num1 < 0 && num2 < 0;
    }

    //TODO emojis
    private String preprocess(String word) {
        String removedPunctuation = word.replaceAll("\\p{Punct}", "");
        return removedPunctuation.toLowerCase(Locale.ROOT);
    }


    private void createSentimentDictionary() throws IOException {
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

    private void createNegationList() throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(negation_lexicon))) {
            stream.forEach(line -> {
                negationDictionary.add(line.toLowerCase(Locale.ROOT));
            });
        }
    }

    private void createIntensityList() throws IOException {
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

    private void createEmojiDictionary() throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(emoji_lexicon))) {
            stream.forEach(line -> {
                String[] components = line.split(",");
                if (components.length >= 3) {
                    double score = Double.parseDouble(components[2]);
                    emojiDictionary.put(components[1].toLowerCase(Locale.ROOT), score);
                }
            });
        }
    }


}

