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
    private int numNoSentimentDetected = 0;


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
            evaluate(true);
            evaluate(false);

        } catch (IOException ex) {
            ex.printStackTrace();

        }
    }

    //analyzes tweet using
    public double analyzeTweet(String tweet, boolean normalize, boolean print) {

        double score = 0.0;
        int index = 0;
        boolean sentimentWord = false;
        if (print)
            System.out.println(ANSI_GREEN + "Analyzing tweet: " + tweet + ANSI_RESET);
        /*String processedTweet = preprocess(tweet);

        List<String> words = new ArrayList<>(Arrays.asList(processedTweet.split(" ")));

         */
        List<String> unprocessedWords = new ArrayList<>(Arrays.asList(tweet.split(" ")));
        List<String> words = new ArrayList<>();
        unprocessedWords.forEach(word -> words.add(preprocess(word)));

        for (int i = 0; i < words.size(); i++) {


            String word = words.get(i);
            if (sentimentDictionary.containsKey(word)) {
                sentimentWord = true;
                if (word.equals("no")) {
                    if (words.size() - 1 > index) {
                        if (sentimentDictionary.containsKey(words.get(index + 1))) {
                            //do not add score as it is a negation
                            index++;
                            continue;
                        }
                    }
                }
                int polarity = 1;
                for (int tempIndex = index - 1; tempIndex >= 0 && index - 2 <= tempIndex; tempIndex--) {
                    if (negationDictionary.contains(words.get(tempIndex))) {
                        polarity *= -1;
                        if (print)
                            System.out.println("Detected negation word " + words.get(tempIndex));
                    } else if (intensityDictionary.containsKey(words.get(tempIndex))) {
                        polarity *= intensityDictionary.get(words.get(tempIndex));
                    }
                }
                if (!(sentimentDictionary.get(word) < 0 && polarity < 0)) {
                    score += polarity * sentimentDictionary.get(word);
                } else {
                    System.out.println("lol");
                }

                if (print)
                    System.out.println("Detected sentiment word " + word + " with polarity " + polarity * sentimentDictionary.get(word));

            } else if (emojiDictionary.containsKey(word)) {
                score += emojiDictionary.get(word);
                sentimentWord = true;
                //System.out.println("Detected emoji " + word + " with polarity " + emojiDictionary.get(word));
                //😯
            } else {
                //try to match word still

                for (int t = 1; t < word.length() - 2 && t < 3; t++) {
                    String testWord = word.substring(0, word.length() - t);
                    if (sentimentDictionary.containsKey(testWord)) {
                        //System.out.println("Converted " + word + " to " + testWord);
                        //do better here, because of negation etc.
                        if (i >= 2) {//do better here
                            //words.add(words.get());
                        }
                        words.add(testWord);
                        break;
                    }
                }


            }

            index++;
        }
        if (!sentimentWord) {
            //System.out.println("No sentiment word detected in sentence " + words);
            numNoSentimentDetected++;
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
        if (score <= -0.5) {
            return -1;
        } else if (score >= 0.5) {
            return 1;
        } else {
            return 0;
        }
    }

    public void calculateMeasures(List<TrainingTweet> tweets, boolean neutral) {
        int correctTweets = 0;
        int falseTweets = 0;
        double trueneutral = 0;
        double truenegative = 0;
        double truepositive = 0;

        double falseNeutralForPositive = 0;
        double falseNeutralForNegative = 0;
        double falseNegativeForNeutral = 0;
        double falseNegativeForPositive = 0;

        double falsePositiveForNeutral = 0;
        double falsePositiveForNegative = 0;

        int numDetectedNeutral = 0;
        int numIncorrectNeutral = 0;
        int numTotalNeutral = 0;

        int numTrueNeutral = 0;
        int numRoundedNeutral = 0;

        numNoSentimentDetected = 0;

        for (TrainingTweet tweet : tweets) {
            double correctScore = tweet.getScore();
            if (correctScore == 0.0) {
                if (neutral) {
                    numTotalNeutral++;
                } else {
                    continue;
                }
            }
            double lexiconScore = analyzeTweet(tweet.getText(), false, false);
            if (lexiconScore == 0.0) {
                numTrueNeutral++;
            }
            lexiconScore = normalizeScore(lexiconScore);
            if (lexiconScore == 0.0) {
                numRoundedNeutral++;
            }

            int lexiconRounded = (int) (lexiconScore * 10) / 10;

            if (lexiconScore == 0.0) {
                numDetectedNeutral++;
            }

            boolean correct = lexiconScore == correctScore;

            if (correct) {
                correctTweets++;
                switch (lexiconRounded) {
                    case -1 -> truenegative++;
                    case 0 -> trueneutral++;
                    case 1 -> truepositive++;
                }
            } else {
                falseTweets++;
                switch (lexiconRounded) {
                    case -1:
                        if (correctScore == 1)
                            falseNegativeForPositive++;
                        else
                            falseNegativeForNeutral++;
                        break;
                    case 0:
                        numIncorrectNeutral++;
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

        numRoundedNeutral -= numTrueNeutral;


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
        System.out.println(neutralPrecison + " " + negativePrecision + " " + positivePrecision);
        System.out.println(neutralRecall + " " + negativeRecall + " " + postiveRecall);

        System.out.println("Lexicon Method Evaluation: ");
        int totalInstances = correctTweets + falseTweets;
        double correctPercentage = (double) correctTweets / totalInstances * 100;
        double falsePercentage = (double) falseTweets / totalInstances * 100;

        System.out.println("Correctly classified instances:\t " + correctTweets + "\t " + correctPercentage + "%");
        System.out.println("Incorrectly classified instances:\t " + falseTweets + "\t " + falsePercentage + "%");
        System.out.println("Total number instances:\t " + totalInstances);

        System.out.println("Correct number of neutral instances: " + numTotalNeutral);
        System.out.println("Neutral instances detected by lexicon score: " + numDetectedNeutral);
        System.out.println("Of which incorrect (should be positive or negative): " + numIncorrectNeutral);
        System.out.println("No sentiment detected: " + numNoSentimentDetected);
        System.out.println("True neutral (0.0): " + numTrueNeutral);
        System.out.println("Rounded neutral: " + numRoundedNeutral);


    }

    public void evaluate(boolean neutral) {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        List<TrainingTweet> tweets = session.createQuery("from TrainingTweet ", TrainingTweet.class).list();
        List<TrainingTweet> dataset1 = new ArrayList<>();
        List<TrainingTweet> dataset2 = new ArrayList<>();
        tweets.forEach(trainingTweet -> {
            if (trainingTweet.getId() <= 4242) {
                dataset1.add(trainingTweet);
            } else {
                dataset2.add(trainingTweet);
            }
        });

        //calculateMeasures(dataset1, neutral);
        //calculateMeasures(dataset2, neutral);
        calculateMeasures(tweets, neutral);

    }

    private String preprocess(String word) {
        if (!emojiDictionary.containsKey(word)) {
            word = word.replaceAll("\\p{Punct}", "");
        }
        //removedPunctuation = removedPunctuation.toLowerCase(Locale.ROOT);
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



/*
    //TODO emojis
    private String preprocess(String tweet) {
        tweet = tweet.replaceAll("\\p{Punct}", " ");
        tweet = tweet.replaceAll("http://[\\S]+|https://[\\S]+", "");
        tweet = tweet.toLowerCase(Locale.ROOT);
        String[] words = tweet.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            word = word.replaceAll("(&amp)|(&quot)|(#)", "");
            word = removeTripleChar(word);
            sb.append(word).append(" ");

        }
        return sb.toString();
    }

    private String removeTripleChar(String word) {
        StringBuilder sb = new StringBuilder();
        if (word.length() >= 1) {
            char previousChar = word.charAt(0);
            int count = 1;
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

 */

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

