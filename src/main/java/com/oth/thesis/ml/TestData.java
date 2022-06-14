package com.oth.thesis.ml;

import com.oth.thesis.database.TestTweet;
import com.oth.thesis.twitter.TwitterCrawler;
import com.oth.thesis.twitter.TwitterData;
import com.oth.thesis.twitter.TwitterResponse;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class TestData {
    private final static String data1 = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\tweeti-b.dist.tsv";
    private final static String data2 = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\twitter4242.txt";


    public static void create(SessionFactory sessionFactory) {
        try {
            parseData2(sessionFactory);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void parseData1(SessionFactory sessionFactory) throws IOException {
        TwitterCrawler crawler = new TwitterCrawler();

        try (Stream<String> stream = Files.lines(Paths.get(data1))) {
            stream.forEach(line -> {
                String[] components = line.split("\t");
                //tweetID, userID, score
                if (components.length == 3) {
                    try {
                        TwitterResponse response = crawler.getTweet(components[0]);
                        if (response != null) {
                            if (response.status == 429) {
                                Thread.sleep(16 * 60 * 1000);
                            } else if (response.data != null) {
                                if (!response.data.isEmpty()) {
                                    TwitterData data = response.data.iterator().next();

                                    Session session = sessionFactory.openSession();
                                    session.beginTransaction();
                                    double score = 0;
                                    switch (components[2]) {
                                        case "\"objective\"":
                                            break;
                                        case "\"neutral\"":
                                            break;
                                        case "\"positive\"":
                                            score = 1.0;
                                            break;
                                        case "\"negative\"":
                                            score = -1.0;
                                            break;
                                        case "\"objective-OR-neutral\"":
                                            break;
                                        default:
                                            break;
                                    }
                                    session.saveOrUpdate(new TestTweet(data.id, data.text, score));
                                    session.getTransaction().commit();
                                    session.close();
                                }
                            }
                        }
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            });
        }
    }

    private static void parseData2(SessionFactory sessionFactory) throws IOException {
        AtomicInteger countPositve = new AtomicInteger(0);
        AtomicInteger countNegative = new AtomicInteger(0);
        AtomicInteger countNeutral = new AtomicInteger(0);
        AtomicLong id = new AtomicLong(0);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(data2), StandardCharsets.UTF_8));
            Stream<String> lines = reader.lines();
            lines.forEach(line -> {
                String[] components = line.split("\t");
                //tweetID, userID, score
                if (components.length == 3) {
                    Session session = sessionFactory.openSession();
                    session.beginTransaction();
                    double score = 0;
                    int meanPos = Integer.parseInt(components[0]);
                    int meanNeg = Integer.parseInt(components[1]);


                    if (meanPos >= 1.5 * meanNeg) {
                        score = 1.0;
                        countPositve.incrementAndGet();
                    } else if (meanNeg >= 1.5 * meanPos) {
                        score = -1.0;
                        countNegative.incrementAndGet();
                    } else {
                        countNeutral.incrementAndGet();
                    }


                    session.saveOrUpdate(new TestTweet(id.incrementAndGet(), components[2], score));
                    session.getTransaction().commit();
                    session.close();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Positive: " + countPositve.get());
        System.out.println("Negative: " + countNegative.get());
        System.out.println("Neutral: " + countNeutral.get());

    }
}

