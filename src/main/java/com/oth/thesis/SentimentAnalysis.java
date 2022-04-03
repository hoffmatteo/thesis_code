package com.oth.thesis;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class SentimentAnalysis {

    public static void main(String[] args) {
        try {
            SessionFactory factory = startDatabase();
            LexiconMethod lexiconMethod = new LexiconMethod(factory);
            //MethodML nb = new MethodML(factory);
            //TrainingData.create(factory);
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
