package com.oth.thesis;

import com.oth.thesis.twitter.TwitterCrawler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class LexiconMethod {
    private static final String sentiment_lexicon = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\vader_lexicon.txt";
    private static final String negation_lexicon = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\negations.txt";
    private static final String intensity_lexicon = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\intensities.txt";
    private static final Map<String, Double> sentimentDictionary = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final List<String> negationDictionary = new ArrayList<>();
    private static final Map<String, Double> intensityDictionary = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);


    //TODO negation has not --> doesnt make sense
    //TODO negation no is also sentiment word?


    public static void main(String[] args) {
        try {
            createSentimentDictionary();
            createNegationList();
            createIntensityList();
            TwitterCrawler crawler = new TwitterCrawler();
            crawler.crawlTweets(100, "ukraine");
            //analyse("very not good");
        } catch (IOException ex) {
            ex.printStackTrace();

        }
    }

    public static void analyse(String tweet) {
        String[] words = tweet.split(" ");
        double score = 0.0;
        int index = 0;
        //TODO better
        for (int i = 0; i < words.length; i++) {
            words[i] = preprocess(words[i]);
        }
        for (String word : words) {
            if (sentimentDictionary.containsKey(word)) {
                if (word.equals("no")) {
                    if (words.length > index && sentimentDictionary.containsKey(words[index + 1])) {
                        //do not add score as it is a negation
                        index++;
                        continue;
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
            }
            index++;
        }
        //TDOo
        System.out.println(TwitterCrawler.ANSI_BLUE + "Final score: " + score + TwitterCrawler.ANSI_RESET);
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
                    intensityDictionary.put(components[0], score);
                }
            });
        }
    }
}

