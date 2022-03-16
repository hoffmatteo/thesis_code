package com.oth.thesis;

import com.oth.thesis.database.TrainingTweet;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ConverterUtils;
import weka.core.tokenizers.NGramTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NaiveBayes {
    private static final String arff_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\trainingTweets.arff";
    private SessionFactory sessionFactory;

    public NaiveBayes(SessionFactory sessionFactory) throws Exception {
        this.sessionFactory = sessionFactory;
        //buildArff();
        train();


    }

    private void classify(String sentence) {

    }


    public void train() throws Exception {
        Instances trainingData = getDataset();
        NaiveBayesMultinomial classifier = new NaiveBayesMultinomial();
        System.out.println(trainingData.classIndex());
        classifier.buildClassifier(trainingData);

        File f = new File(arff_file);


        ArffLoader loader = new ArffLoader();
        loader.setSource(f);
        Instances oldData = loader.getDataSet();
        oldData.setClassIndex(0);


        ArrayList<Attribute> atts = new ArrayList<>();
        List<String> nominalValues = Arrays.asList("-1.0", "0.0", "1.0");
        atts.add(new Attribute("calculatedScore", nominalValues));
        atts.add(new Attribute("tweetText", (ArrayList<String>) null));
        Instances data = new Instances("MyRelation", atts, 0);

        double[] vals = new double[data.numAttributes()];
        // - numeric
        //change this to nominal --> -1 oder 0 or 1
        //vals[0] = data.attribute(0).addStringValue(String.valueOf(tweet.getScore()));
        //vals[0] = nominalValues.indexOf(String.valueOf(tweet.getScore()));
        // - string
        vals[1] = data.attribute(1).addStringValue("@WorldWideWob Lebron so scared of big moments");

        data.add(new DenseInstance(1.0, vals));

        // - numeric
        //change this to nominal --> -1 oder 0 or 1
        //vals[0] = data.attribute(0).addStringValue(String.valueOf(tweet.getScore()));
        //vals[0] = nominalValues.indexOf(String.valueOf(tweet.getScore()));
        // - string

        data.add(new DenseInstance(1.0, vals));

        data.setClassIndex(0);


        StringToWordVector filter = new StringToWordVector();
        filter.setAttributeIndices("last");
        /**
         * Add ngram tokenizer to filter with min and max length set to 1
         */
        NGramTokenizer tokenizer = new NGramTokenizer();
        tokenizer.setNGramMinSize(1);
        tokenizer.setNGramMaxSize(1);
        /**
         * Tokenize based on delimiter
         */
        tokenizer.setDelimiters("\\W");
        filter.setTokenizer(tokenizer);
        /**
         * To lowercase converting
         */
        filter.setLowerCaseTokens(true);
        filter.setInputFormat(oldData);

        data = Filter.useFilter(data, filter);
        System.out.println("test " + data);


        double index = classifier.classifyInstance(data.instance(0));
        System.out.println(data.instance(1));
        System.out.println("Index: " + index);
        String className = trainingData.attribute(0).value((int) index);
        double distribution[] = classifier.distributionForInstance(data.instance(1));
        System.out.println("Class: " + className);
        System.out.println(Arrays.toString(distribution));


    }

    public Instances getDataset() throws Exception {
        File f = new File(arff_file);
        if (f.exists()) {
            ArffLoader loader = new ArffLoader();
            loader.setSource(f);
            Instances data = loader.getDataSet();
            data.setClassIndex(0);

            StringToWordVector filter = new StringToWordVector();
            filter.setAttributeIndices("last");
            /**
             * Add ngram tokenizer to filter with min and max length set to 1
             */
            NGramTokenizer tokenizer = new NGramTokenizer();
            tokenizer.setNGramMinSize(1);
            tokenizer.setNGramMaxSize(1);
            /**
             * Tokenize based on delimiter
             */
            tokenizer.setDelimiters("\\W");
            filter.setTokenizer(tokenizer);
            /**
             * To lowercase converting
             */
            filter.setLowerCaseTokens(true);
            filter.setInputFormat(data);

            data = Filter.useFilter(data, filter);
            System.out.println(data);

            return data;
        } else {
            throw new Exception();
        }
    }

    public void buildArff() throws Exception {
        ArrayList<Attribute> atts = new ArrayList<>();
        List<String> nominalValues = Arrays.asList("-1.0", "0.0", "1.0");
        atts.add(new Attribute("calculatedScore", nominalValues));
        atts.add(new Attribute("tweetText", (ArrayList<String>) null));
        Instances data = new Instances("MyRelation", atts, 0);

        Session session = sessionFactory.openSession();
        session.beginTransaction();
        List<TrainingTweet> result = session.createQuery("from TrainingTweet ", TrainingTweet.class).list();
        result.forEach(tweet -> {
            double[] vals = new double[data.numAttributes()];
            // - numeric
            //change this to nominal --> -1 oder 0 or 1
            //vals[0] = data.attribute(0).addStringValue(String.valueOf(tweet.getScore()));
            vals[0] = nominalValues.indexOf(String.valueOf(tweet.getScore()));
            // - string
            vals[1] = data.attribute(1).addStringValue(tweet.getText());

            data.add(new DenseInstance(1.0, vals));

        });

        System.out.println(data);


        try {
            ConverterUtils.DataSink.write(arff_file, data);
        } catch (Exception e) {
            System.err.println("Failed to save data to: " + arff_file);
            e.printStackTrace();
        }
    }
}
