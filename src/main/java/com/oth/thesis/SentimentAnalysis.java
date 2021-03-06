package com.oth.thesis;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class SentimentAnalysis {

    public static void main(String[] args) {
        try {
            SessionFactory factory = startDatabase();
            //LexiconMethod lexiconMethod = new LexiconMethod(factory);
            //MLMethod nb = new MLMethod(factory);
            //TrainingData.create(factory);
            //MLMethod nb = new MLMethod(factory);
            HybridMethod hybridMethod = new HybridMethod(factory);
            //hybridMethod.try1();
            hybridMethod.try2();


            //TwitterCrawler crawler = new TwitterCrawler();
            //crawler.crawlTweets(1000000, "devin booker", factory);
            //crawler.crawlTweets(1000000, "chris paul", factory);
            //crawler.crawlTweets(1000000, "cp3", factory);


            //MLMethod nb = new MLMethod(factory);
            //HybridMethod hm = new HybridMethod(factory);
            //hm.try1();


        } catch (Exception e) {
            e.printStackTrace();

        }


    }

    private static SessionFactory startDatabase() throws Exception {
// configures settings from hibernate.cfg.xml
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure().build();
        return new MetadataSources(registry).buildMetadata().buildSessionFactory();

    }
}
