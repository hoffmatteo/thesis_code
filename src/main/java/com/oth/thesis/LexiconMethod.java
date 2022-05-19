package com.oth.thesis;

import com.oth.thesis.database.AnalyzedTweet;
import com.oth.thesis.database.TrainingTweet;
import com.oth.thesis.twitter.TwitterCrawler;
import com.vader.sentiment.analyzer.SentimentAnalyzer;
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
            evaluate(false);
        } catch (IOException ex) {
            ex.printStackTrace();

        }
    }

    public double analyzeTweet(String tweet, boolean normalize, boolean print) {
        String[] words = tweet.split(" ");
        double score = 0.0;
        int index = 0;
        if (print)
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
                        if (print)
                            System.out.println("Detected negation word " + words[tempIndex]);
                    } else if (intensityDictionary.containsKey(words[tempIndex])) {
                        polarity *= intensityDictionary.get(words[tempIndex]);
                    }
                }
                score += polarity * sentimentDictionary.get(word);
                if (print)
                    System.out.println("Detected sentiment word " + word + " with polarity " + polarity * sentimentDictionary.get(word));

            } else if (emojiDictionary.containsKey(word)) {
                score += emojiDictionary.get(word);
                //System.out.println("Detected emoji " + word + " with polarity " + emojiDictionary.get(word));
                //😯
            }

            index++;
        }
        if (print)
            System.out.println(TwitterCrawler.ANSI_BLUE + "Final score: " + score + ANSI_RESET);
        if (normalize) {
            return normalizeScore(score);
        }
        return score;


    }

    public void analyzeTweets() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        List<AnalyzedTweet> result = session.createQuery("from AnalyzedTweet ", AnalyzedTweet.class).list();
        result.forEach(tweet -> {

            double score = analyzeTweet(tweet.getText(), false, false);
            tweet.setLexicon_score(score);
            session.saveOrUpdate(tweet);
        });
        session.getTransaction().commit();
        session.close();


    }

    private double normalizeScore(double score) {
        if (score <= -1) {
            return -1;
        } else if (score >= 1) {
            return 1;
        } else {
            return 0;
        }
    }

    public void evaluate(boolean neutral) {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        List<TrainingTweet> tweets = session.createQuery("from TrainingTweet ", TrainingTweet.class).list();
        AtomicInteger falseTweets = new AtomicInteger(0);
        AtomicInteger correctTweets = new AtomicInteger(0);
        //Problem: Neutral!
        double trueneutral = 0;
        double truenegative = 0;
        double truepositive = 0;

        double falseNeutralForPositive = 0;
        double falseNeutralForNegative = 0;
        double falseNegativeForNeutral = 0;
        double falseNegativeForPositive = 0;

        double falsePositiveForNeutral = 0;
        double falsePositiveForNegative = 0;


        for (int i = 0; i < tweets.size(); i++) {
            double lexiconScore = analyzeTweet(tweets.get(i).getText(), true, false);
            int lexiconRounded = (int) (lexiconScore * 10) / 10;
            double correctScore = tweets.get(i).getScore();
            if (correctScore == 0.0 && !neutral) {
                continue;
            }
            //TP, TN, FP, FN
            boolean correct = lexiconScore == correctScore;
            if (correct) {
                switch (lexiconRounded) {
                    case -1 -> truenegative++;
                    case 0 -> trueneutral++;
                    case 1 -> truepositive++;
                }
            } else {
                switch (lexiconRounded) {
                    case -1:
                        if (correctScore == 1)
                            falseNegativeForPositive++;
                        else
                            falseNegativeForNeutral++;
                        break;
                    case 0:
                        if (correctScore == 1)
                            falseNeutralForPositive++;
                        else
                            falseNeutralForNegative++;
                        break;
                    case 1:
                        if (correctScore == -1)
                            falsePositiveForNegative++;
                        else
                            falsePositiveForNeutral++;
                        break;
                }
            }
        }

        double neutralPrecison = trueneutral / (trueneutral + falseNeutralForPositive + falseNeutralForNegative);
        double neutralRecall = trueneutral / (trueneutral + falseNegativeForNeutral + falsePositiveForNeutral);
        double neutralFScore = (2 * neutralPrecison * neutralRecall) / (neutralPrecison + neutralRecall);

        double negativePrecision = truenegative / (truenegative + falseNegativeForPositive + falseNegativeForNeutral);
        double negativeRecall = truenegative / (truenegative + falsePositiveForNegative + falseNeutralForNegative);
        double negativeFScore = (2 * negativePrecision * negativeRecall) / (negativePrecision + negativeRecall);

        double positivePrecision = truepositive / (truepositive + falsePositiveForNegative + falsePositiveForNeutral);
        double postiveRecall = truepositive / (truepositive + falseNeutralForPositive + falseNegativeForPositive);
        double positiveFScore = (2 * positivePrecision * postiveRecall) / (positivePrecision + postiveRecall);

        System.out.println(neutralFScore + " " + negativeFScore + " " + positiveFScore);

        System.out.println(neutralFScore + " " + negativeFScore + " " + positiveFScore);
        System.out.println(neutralPrecison + " " + negativePrecision + " " + positivePrecision);
        System.out.println(neutralRecall + " " + negativeRecall + " " + postiveRecall);





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


        trueneutral = 0;
        truenegative = 0;
        truepositive = 0;

        falseNeutralForPositive = 0;
        falseNeutralForNegative = 0;
        falseNegativeForNeutral = 0;
        falseNegativeForPositive = 0;

        falsePositiveForNeutral = 0;
        falsePositiveForNegative = 0;


        for (int i = 0; i < tweets.size(); i++) {
            double correctScore = tweets.get(i).getScore();
            if (correctScore == 0.0 && !neutral) {
                continue;
            }
            double vaderScore = SentimentAnalyzer.getScoresFor(tweets.get(i).getText()).getCompoundPolarity();
            if (vaderScore >= 0.05) {
                vaderScore = 1.0;
            } else if (vaderScore <= -0.05) {
                vaderScore = -1.0;
            } else {
                vaderScore = 0.0;
            }
            int vaderScoreRounded = (int) vaderScore;
            boolean correct = vaderScore == correctScore;
            if (correct) {
                switch (vaderScoreRounded) {
                    case -1 -> truenegative++;
                    case 0 -> trueneutral++;
                    case 1 -> truepositive++;
                }
            } else {
                switch (vaderScoreRounded) {
                    case -1:
                        if (correctScore == 1)
                            falseNegativeForPositive++;
                        else
                            falseNegativeForNeutral++;
                        break;
                    case 0:
                        if (correctScore == 1)
                            falseNeutralForPositive++;
                        else
                            falseNeutralForNegative++;
                        break;
                    case 1:
                        if (correctScore == -1)
                            falsePositiveForNegative++;
                        else
                            falsePositiveForNeutral++;
                        break;
                }
            }
        }

        neutralPrecison = trueneutral / (trueneutral + falseNeutralForPositive + falseNeutralForNegative);
        neutralRecall = trueneutral / (trueneutral + falseNegativeForNeutral + falsePositiveForNeutral);
        neutralFScore = (2 * neutralPrecison * neutralRecall) / (neutralPrecison + neutralRecall);

        negativePrecision = truenegative / (truenegative + falseNegativeForPositive + falseNegativeForNeutral);
        negativeRecall = truenegative / (truenegative + falsePositiveForNegative + falseNeutralForNegative);
        negativeFScore = (2 * negativePrecision * negativeRecall) / (negativePrecision + negativeRecall);

        positivePrecision = truepositive / (truepositive + falsePositiveForNegative + falsePositiveForNeutral);
        postiveRecall = truepositive / (truepositive + falseNeutralForPositive + falseNegativeForPositive);
        positiveFScore = (2 * positivePrecision * postiveRecall) / (positivePrecision + postiveRecall);


        System.out.println(neutralFScore + " " + negativeFScore + " " + positiveFScore);
        System.out.println(neutralPrecison + " " + negativePrecision + " " + positivePrecision);
        System.out.println(neutralRecall + " " + negativeRecall + " " + postiveRecall);



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
        //totalInstances = positiveCount + falseCount;
        //correctPercentage = (double) positiveCount / totalInstances * 100;
        //falsePercentage = (double) falseCount / totalInstances * 100;

        //System.out.println("Correctly classified instances:\t " + positiveCount + "\t " + correctPercentage + "%");
        //System.out.println("Incorrectly classified instances:\t " + falseCount + "\t " + falsePercentage + "%");
        //System.out.println("Total number instances:\t " + totalInstances);


    }

    private boolean sameSign(double num1, double num2) {
        return num1 >= 0 && num2 >= 0 || num1 < 0 && num2 < 0;
    }

    //TODO emojis
    private String preprocess(String word) {
        String removedPunctuation = word.replaceAll("\\p{Punct}", "");
        removedPunctuation = removedPunctuation.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();

        if (word.length() >= 1) {
            char previousChar = word.charAt(0);
            int count = 1;
            //word.replaceAll("")
            int idx;
            sb.append(previousChar);

            for (int i = 1; i < word.length(); i++) {
                char c = word.charAt(i);
                if (previousChar == c) {
                    count++; //
                    if (count < 3) {
                        sb.append(c);
                    }
                } else {
                    sb.append(c);
                    count = 1;
                }
                previousChar = c;
            }
        }
        return sb.toString();

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

