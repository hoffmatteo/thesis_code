package com.oth.thesis;

import com.oth.thesis.database.TestTweet;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.Logistic;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ArffLoader;
import weka.core.converters.ConverterUtils;
import weka.core.stemmers.LovinsStemmer;
import weka.core.tokenizers.NGramTokenizer;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class MLMethod {
    public static String test_arff_path = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\testing\\testTweetsNominal.arff";
    public static String nb_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\maximum\\nbmulti.model";
    private static final String logistic_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\equal\\logistic.model";
    private static final String svm_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\equal\\svm.model";
    private static final String forest_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\maximum\\forest.model";
    public static final String training_path = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\training\\training.1600000.processed.noemoticon.csv";
    private static final String dictionary_path = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\dictionary.txt";
    private static final String filteredWordsPath = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\lexicons\\filteredWords.txt";
    private static String training_arff_path = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\training\\traindata.arff";


    private LexiconMethod lexiconMethod;
    public boolean isLexiconMethod = false;
    public boolean isLexiconMethod2 = false;


    private final SessionFactory sessionFactory;
    private Instances trainingData;
    private Instances testData;
    StringToWordVector filter = new StringToWordVector();


    public MLMethod(SessionFactory sessionFactory) throws Exception {
        this.sessionFactory = sessionFactory;
        init();
        //buildArffTrain(true);
        runNaiveBayes();
        //runLogisticRegression(95000);
        //runRandomForest(120000);
        //runSVM(1000000);

    }


    public MLMethod(SessionFactory sessionFactory, LexiconMethod lexiconMethod, boolean isMethod2) throws Exception {
        this.sessionFactory = sessionFactory;
        if (isMethod2) {
            isLexiconMethod2 = true;
            nb_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\hybrid\\nbMethodTEST.model";
            training_arff_path = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\traindataHybridMethodTEST.arff";
        } else {
            isLexiconMethod = true;
            nb_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\hybrid\\nbMethod1.model";
            training_arff_path = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\traindataHybridMethod1.arff";
            test_arff_path = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\testTweetsHybrid1.arff";
            buildArffTest();
        }

        this.lexiconMethod = lexiconMethod;
        buildArffTrain();
        init();
        runNaiveBayes();
    }

    private void init() throws Exception {
        filter.setLowerCaseTokens(true);
        filter.setDoNotOperateOnPerClassBasis(true);
        filter.setOptions(new String[]{"-W", "15000", "-M", "20", "-dictionary", dictionary_path, "-L"});
        NGramTokenizer tokenizer = new NGramTokenizer();
        tokenizer.setNGramMinSize(1);
        tokenizer.setNGramMaxSize(2);


        filter.setTokenizer(tokenizer);
        filter.setStemmer(new LovinsStemmer());

        buildInstancesTest();


    }

    public void runNaiveBayes() throws Exception {
        buildInstancesTrain(95000);

        boolean nominal = true;
        NaiveBayesMultinomial nb = new NaiveBayesMultinomial();
        //NaiveBayes nb = new NaiveBayes();
        //nb.setOptions(new String[]{"-K", ""});

        train(nb, nb_file);
        test(nb_file, nominal);
    }

    public void runLogisticRegression(int limit) throws Exception {
        boolean nominal = true;
        buildInstancesTrain(limit);

        Logistic log = new Logistic();
        log.setOptions(new String[]{"-S", "-M", "10"});

        //train(log, logistic_file);
        test(logistic_file, nominal);

    }

    public void runSVM(int limit) throws Exception {
        boolean nominal = true;
        buildInstancesTrain(limit);

        //buildInstancesTrain(nominal);
        //buildArffTest(nominal);
        //buildInstancesTest(nominal);
        LibSVM svm = new LibSVM();
        //linear
        svm.setOptions(new String[]{"-K", "0"});

        train(svm, svm_file);
        test(svm_file, nominal);

    }

    public void runRandomForest(int limit) throws Exception {
        buildInstancesTrain(limit);

        boolean nominal = true;
        RandomForest forest = new RandomForest();
        forest.setOptions(new String[]{"-depth", "300"});
        train(forest, forest_file);
        test(forest_file, nominal);

    }

    public void test(String modelFile, boolean nominal) throws Exception {
        FilteredClassifier classifier = (FilteredClassifier) SerializationHelper.read(modelFile);
        evaluate(classifier, nominal);

    }


    public void train(Classifier classifier, String modelFile) throws Exception {
        FilteredClassifier filteredClassifier = new FilteredClassifier();
        filteredClassifier.setClassifier(classifier);
        filteredClassifier.setFilter(filter);


        filteredClassifier.buildClassifier(trainingData);

        weka.core.SerializationHelper.write(modelFile, filteredClassifier);


    }

    public void evaluate(FilteredClassifier classifier, boolean nominal) throws Exception {
        Evaluation eval = new Evaluation(trainingData);


        eval.evaluateModel(classifier, testData);

        System.out.println("** " + classifier.getClassifier().getClass() + " Evaluation with Datasets: " + trainingData.numInstances() + " **");

        System.out.println(eval.toSummaryString());
        /*
        for (int i = 0; i < 50; i++) {
            //System.out.println(testData.instance(i));
            System.out.println(unfilteredTestData.instance(i));
            System.out.println(testData.instance(i));

            double result = classifier.classifyInstance(testData.instance(i));
            String className;
            if (nominal) {
                className = trainingData.attribute(0).value((int) result);
            } else {
                className = String.valueOf(result);
            }
            double[] distribution = classifier.distributionForInstance(testData.instance(i));
            System.out.print("Class: " + className + ", Distribution: ");
            System.out.println(Arrays.toString(distribution));

        }

         */

        System.out.println("Confusion Matrix: " + Arrays.deepToString(eval.confusionMatrix()));
        System.out.println("Recall: " + eval.recall(1));
        System.out.println("Precision: " + eval.precision(1));
        System.out.println("F-Measure: " + eval.fMeasure(1));


    }

    public void buildInstancesTest() throws Exception {
        File f = new File(test_arff_path);

        if (f.exists()) {
            ArffLoader loader = new ArffLoader();
            loader.setSource(f);
            Instances data = loader.getDataSet();
            data.setClassIndex(0);
            testData = data;

            System.out.println("Test filtered Attributes: " + testData.numAttributes());
            System.out.println("Test Instances: " + testData.numInstances());

        } else {
            throw new Exception();
        }
    }

    public void buildArffTest() throws Exception {
        Instances data = buildArff("TestTweets");
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        List<TestTweet> result = session.createQuery("from TestTweet ", TestTweet.class).list();
        AtomicInteger counterPositive = new AtomicInteger(0);
        result.forEach(tweet -> {
            if (tweet.getScore() == 0.0) {
                return;
            }

            double[] vals = new double[data.numAttributes()];
            vals[0] = data.attribute(0).indexOfValue(String.valueOf(tweet.getScore()));

            String text = tweet.getText().toLowerCase(Locale.ROOT);
            text = preprocess(text);
            vals[1] = data.attribute(1).addStringValue(text);

            if (isLexiconMethod) {
                double score = lexiconMethod.analyzeTweet(tweet.getText(), true, false);
                score += 1;
                vals[2] = score;
            }
            data.add(new DenseInstance(1.0, vals));
            data.setClassIndex(0);
        });
        System.out.println("Testing: " + counterPositive.get() + " positive tweets");
        try {
            ConverterUtils.DataSink.write(test_arff_path, data);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Instances buildArff(String name) {
        ArrayList<Attribute> atts;
        if (isLexiconMethod) {
            atts = new ArrayList<>(3);
        } else {
            atts = new ArrayList<>(2);
        }

        List<String> nominalValues = new ArrayList<String>(2);
        nominalValues.add("-1.0");
        nominalValues.add("1.0");
        atts.add(new Attribute("calculatedScore", nominalValues));

        atts.add(new Attribute("tweetText", (ArrayList<String>) null));
        if (isLexiconMethod) {
            atts.add(new Attribute("lexiconScore"));
        }
        return new Instances(name, atts, 0);
    }

    private void buildArffTrain() throws Exception {
        Instances trainData = buildArff("TrainingData");
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(training_path), StandardCharsets.UTF_8));

        reader.lines().forEach(line -> {
            String[] components = line.split("\",\"");
            if (components.length == 6) {
                for (int i = 0; i < components.length; i++) {
                    components[i] = components[i].replaceAll("\"", "");
                }
                Double score = switch (components[0]) {
                    case "0" -> -1.0;
                    case "4" -> 1.0;
                    default -> null;
                };
                if (score == null) {
                    return;
                }
                String text = components[5];
                double[] vals = new double[trainData.numAttributes()];

                if (isLexiconMethod2) {
                    double lexiconScore = lexiconMethod.analyzeTweet(text, true, false);
                    if (lexiconScore == 0.0) {
                        return;
                    }
                    vals[0] = trainData.attribute(0).indexOfValue(String.valueOf(lexiconScore));
                } else if (isLexiconMethod) {
                    double lexiconScore = lexiconMethod.analyzeTweet(text, true, false);
                    lexiconScore += 1;
                    vals[2] = lexiconScore;
                    vals[0] = trainData.attribute(0).indexOfValue(String.valueOf(score));
                } else {
                    vals[0] = trainData.attribute(0).indexOfValue(String.valueOf(score));
                }

                text = preprocess(text);
                vals[1] = trainData.attribute(1).addStringValue(text);

                trainData.add(new DenseInstance(1.0, vals));
                trainData.setClassIndex(0);


            }
        });
        System.out.println("Instances: " + trainData.numInstances());

        trainData.setClassIndex(0);

        ConverterUtils.DataSink.write(training_arff_path, trainData);
        trainingData = trainData;

    }


    private void buildInstancesTrain(int limit) throws Exception {

        ArffLoader loader = new ArffLoader();
        loader.setSource(new File(training_arff_path));
        Instances data = loader.getDataSet();
        data.setClassIndex(0);
        trainingData = data;
        if (limit != 0) {
            int positiveInstances = 0;
            int negativeInstances = 0;
            Instances subsetData = new Instances(data, data.numInstances());
            for (int i = 0; i < data.numInstances(); i++) {
                if (data.instance(i).classValue() == 1.0) {
                    if (positiveInstances >= limit / 2) {
                        continue;
                    }
                    positiveInstances++;
                } else {
                    if (negativeInstances >= limit / 2) {
                        continue;
                    }
                    negativeInstances++;
                }
                subsetData.add(data.instance(i));

            }
            trainingData = subsetData;
        }
    }

    private String preprocess(String tweet) {
        List<String> filteredWords = new ArrayList<>();
        try {
            Files.lines(Path.of(filteredWordsPath)).forEach(filteredWords::add);
        } catch (IOException e) {
            e.printStackTrace();
        }
        tweet = tweet.toLowerCase(Locale.ROOT);
        tweet = tweet.replaceAll("http://[\\S]+|https://[\\S]+", "");
        String[] words = tweet.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            word = word.replaceAll("(&amp)|(&quot)|(#)", "");
            if (!filteredWords.contains(word)) {
                sb.append(word).append(" ");
            }
        }
        return sb.toString();
    }

}
