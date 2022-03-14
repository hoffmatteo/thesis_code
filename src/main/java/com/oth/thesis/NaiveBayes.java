package com.oth.thesis;

import com.oth.thesis.database.TrainingTweet;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.util.ArrayList;
import java.util.List;

public class NaiveBayes {
    public NaiveBayes(SessionFactory sessionFactory) throws Exception {
        ArrayList<Attribute> atts = new ArrayList<>();
        atts.add(new Attribute("score"));
        atts.add(new Attribute("text", (ArrayList<String>) null));
        Instances data = new Instances("MyRelation", atts, 0);

        Session session = sessionFactory.openSession();
        session.beginTransaction();
        List<TrainingTweet> result = session.createQuery("from TrainingTweet ", TrainingTweet.class).list();
        result.forEach(tweet -> {
            double[] vals = new double[data.numAttributes()];
            // - numeric
            vals[0] = tweet.getScore();
            // - string
            vals[1] = data.attribute(1).addStringValue(tweet.getText());

            data.add(new DenseInstance(1.0, vals));

        });


        System.out.println(data);

        StringToWordVector filter = new StringToWordVector();
        filter.setInputFormat(data);
        Instances newData = Filter.useFilter(data, filter);
        System.out.println(newData);


    }
}
