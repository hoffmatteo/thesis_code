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
    private static final String emoji_lexicon = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\lexicons\\emojis.csv";
    private final Map<String, Double> sentimentDictionary = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final List<String> negationDictionary = new ArrayList<>();
    private final Map<String, Double> intensityDictionary = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, Double> emojiDictionary = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private SessionFactory sessionFactory;
    private int numNoSentimentDetected = 0;


    public static void main(String[] args) {

    }

    public LexiconMethod(SessionFactory factory) {
        sessionFactory = factory;
        try {
            createSentimentDictionary();
            createNegationList();
            createIntensityList();
            createEmojiDictionary();
            evaluate(true);
            evaluate(false);

        } catch (IOException ex) {
            ex.printStackTrace();

        }
    }

    //analyzes tweet using
    public double analyzeTweet(String tweet, boolean normalize, boolean print) {

        double score = 0.0;
        boolean sentimentWord = false;

        if (print)
            System.out.println(ANSI_GREEN + "Analyzing tweet: " + tweet + ANSI_RESET);

        List<String> unprocessedWords = new ArrayList<>(Arrays.asList(tweet.split(" ")));


        List<String> words = new ArrayList<>();
        unprocessedWords.forEach(word -> words.add(preprocess(word)));
        outerloop:
        for (int index = 0; index < words.size(); index++) {

            String word = words.get(index);

            if (sentimentDictionary.containsKey(word)) {
                sentimentWord = true;
                if (word.equals("no")) {
                    for (int tempIndex = index + 1; tempIndex < words.size(); tempIndex++) {
                        if (sentimentDictionary.containsKey(words.get(tempIndex))) {
                            //do not add score as it is a negation
                            continue outerloop;
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
                score += polarity * sentimentDictionary.get(word);


                if (print)
                    System.out.println("Detected sentiment word " + word + " with polarity " + polarity * sentimentDictionary.get(word));

            } else if (emojiDictionary.containsKey(word)) {
                score += emojiDictionary.get(word);
                sentimentWord = true;
                if (print)
                    System.out.println("Detected emoji " + word + " with polarity " + emojiDictionary.get(word));

            }
        }
        if (!sentimentWord) {
            if (print)
                System.out.println("No sentiment word detected in sentence " + words);
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
        if (score < 0) {
            return -1;
        } else if (score > 0) {
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

            /*
            double lexiconScore = 0;
            double vaderScore = SentimentAnalyzer.getScoresFor(tweet.getText()).getCompoundPolarity();
            if (vaderScore >= 0.05) {
                lexiconScore = 1.0;
            } else if (vaderScore <= -0.05) {
                lexiconScore = -1.0;
            } else {
                lexiconScore = 0.0;
            }

             */

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
                if (lexiconRounded != 0) {
                }
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


        System.out.println("Confusion matrix");
        System.out.println(truepositive + "|" + falseNegativeForPositive + "|" + falseNeutralForPositive);
        System.out.println(falsePositiveForNegative + "|" + truenegative + "|" + falseNeutralForNegative);
        System.out.println(falsePositiveForNeutral + "|" + falseNegativeForNeutral + "|" + trueneutral);


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
        word = word.toLowerCase(Locale.ENGLISH);
        if (!emojiDictionary.containsKey(word)) {
            word = word.replaceAll("\\p{Punct}", "");
        }

        if (sentimentDictionary.containsKey(word)) {
            return word;
        }
        String noTripleChar = removeTripleChar(word);
        if (sentimentDictionary.containsKey(noTripleChar)) {
            return noTripleChar;
        }
        String findBasedOnOriginal = findSentimentWord(word);
        String findBasedOnNoTriple = findSentimentWord(noTripleChar);
        if (sentimentDictionary.containsKey(findBasedOnOriginal)) {
            return findBasedOnOriginal;
        }
        if (sentimentDictionary.containsKey(findBasedOnNoTriple)) {
            return findBasedOnNoTriple;
        }
        return word;
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
            return sb.toString();
        } else {
            return word;
        }
    }

    private String findSentimentWord(String word) {
        //try to match word still
        for (int t = 3; t < word.length(); t++) {
            String testWord = word.substring(0, t);
            if (sentimentDictionary.containsKey(testWord)) {
                //System.out.println("Converted " + word + " to " + testWord);
                //do better here, because of negation etc.
                return testWord;
            }
        }
        return word;
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

