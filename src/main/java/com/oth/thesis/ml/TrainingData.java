package com.oth.thesis.ml;

import com.oth.thesis.database.TrainingTweet;
import com.oth.thesis.twitter.TwitterCrawler;
import com.oth.thesis.twitter.TwitterData;
import com.oth.thesis.twitter.TwitterResponse;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class TrainingData {
    private final static String data1 = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\tweeti-a.dist.tsv";
    private final static String data2 = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\tweeti-b.dist.tsv";


    public static void create(SessionFactory sessionFactory) {
        try {
            parseData1(sessionFactory);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void parseData1(SessionFactory sessionFactory) throws IOException {
        TwitterCrawler crawler = new TwitterCrawler();

        try (Stream<String> stream = Files.lines(Paths.get(data2))) {
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
                                    session.saveOrUpdate(new TrainingTweet(data.id, data.text, score));
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
}