package com.oth.thesis;

import com.oth.thesis.database.CaseStudyTweet;
import com.oth.thesis.database.TrainingTweet;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MLMethod {
    public static final String test_arff_nominal = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\testTweetsNominal.arff";
    public static final String test_arff_nominal_method3 = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\testTweetsNominalMethod3.arff";
    public static final String test_arff_nominal_method32 = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\testTweetsNominalMethod32.arff";
    public static String nb_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\maximum\\nbmultingram.model";
    private static final String logistic_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\maximum\\logistic.model";
    private static final String svm_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\maximum\\svm.model";
    private static final String forest_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\maximum\\forest.model";
    public static final String training_path = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\training.1600000.processed.noemoticon.csv";
    private static final String dictionary_path = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\dictionary.txt";
    private static final String filteredWordsPath = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\lexicons\\filteredWords.txt";
    //private static final String trainDataPath = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\traindata_count.arff";
    private static String trainDataPath = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\traindata.arff";


    private LexiconMethod lexiconMethod;
    public boolean isLexiconMethod = false;
    public boolean isLexiconMethod2 = false;


    private final SessionFactory sessionFactory;
    //TODO better --> differ between nominal here
    private Instances trainingData;
    private Instances testData;
    private Instances unfilteredTestData;
    StringToWordVector filter = new StringToWordVector();


    public MLMethod(SessionFactory sessionFactory) throws Exception {
        this.sessionFactory = sessionFactory;
        init();
        //caseStudy();
        //buildArffTrain(true);
        runNaiveBayes();
        //runLogisticRegression(95000);
        //runRandomForest(120000);
        //runSVM(120000);

    }


    public MLMethod(SessionFactory sessionFactory, LexiconMethod lexiconMethod) throws Exception {
        this.sessionFactory = sessionFactory;

        isLexiconMethod = true;
        //isLexiconMethod2 = true;
        this.lexiconMethod = lexiconMethod;
        nb_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\hybrid\\nbMethod1.model";
        trainDataPath = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\traindataHybridMethod1.arff";
        buildArffTrain(true);
        buildArffTest(true);


        init();


        runNaiveBayes();
        //runLogisticRegression();
        //runRandomForest();
        //runSVM();
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

        buildInstancesTest(true);


    }

    public void runNaiveBayes() throws Exception {
        buildInstancesTrain(true, 0);

        boolean nominal = true;
        NaiveBayesMultinomial nb = new NaiveBayesMultinomial();
        //NaiveBayes nb = new NaiveBayes();
        //nb.setOptions(new String[]{"-K", ""});

        train(nb, nb_file);
        test(nb_file, nominal);
    }

    public void runLogisticRegression(int limit) throws Exception {
        boolean nominal = true;
        buildInstancesTrain(true, limit);

        Logistic log = new Logistic();
        log.setOptions(new String[]{"-S", "-M", "10"});

        //train(log, logistic_file);
        test(logistic_file, nominal);

    }

    public void runSVM(int limit) throws Exception {
        boolean nominal = true;
        buildInstancesTrain(true, limit);

        //buildInstancesTrain(nominal);
        //buildArffTest(nominal);
        //buildInstancesTest(nominal);
        LibSVM svm = new LibSVM();
        //linear
        svm.setOptions(new String[]{"-K", "0"});

        //train(svm, svm_file);
        test(svm_file, nominal);

    }

    public void runRandomForest(int limit) throws Exception {
        buildInstancesTrain(true, limit);

        boolean nominal = true;
        RandomForest forest = new RandomForest();
        forest.setOptions(new String[]{"-depth", "300"});
        //train(forest, forest_file);
        test(forest_file, nominal);

    }

    public void test(String modelFile, boolean nominal) throws Exception {
        FilteredClassifier classifier = (FilteredClassifier) SerializationHelper.read(modelFile);
        evaluate(classifier, nominal);

        /*


        ClassifierAttributeEval attributeEval = new ClassifierAttributeEval();
        attributeEval.setClassifier(classifier.getClassifier());
        Filter test = classifier.getFilter();
        Instances filteredTraining = Filter.useFilter(trainingData, test);
        attributeEval.buildEvaluator(filteredTraining);

        Double infogain = Double.MIN_VALUE;
        ObjectNode objNode;
        int k, i;
        Map<String, Double> ig = new HashMap<>();
        for (int j = 0; j < filteredTraining.numAttributes(); j++) {
            try {
                Double eval = attributeEval.evaluateAttribute(j);
                ig.put(filteredTraining.attribute(j).name(), eval);
            } catch (Exception e) {
                // TODO Auto-generated catch block
            }
        }


        Map<String, Double> result = ig.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        for (Map.Entry<String, Double> entry : result.entrySet()) {
            System.out.println(entry.getKey() + "; " + entry.getValue());
        }

         */


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


        //System.out.println(classifier);

    }

    public void buildInstancesTest(boolean nominal) throws Exception {
        File f;
        if (isLexiconMethod) {
            f = new File(test_arff_nominal_method3);
        } else {
            f = new File(test_arff_nominal);
        }
        if (f.exists()) {
            ArffLoader loader = new ArffLoader();
            loader.setSource(f);
            Instances data = loader.getDataSet();
            data.setClassIndex(0);
            unfilteredTestData = data;

            /*

            Instances subGroup = new Instances(data, data.numInstances());
            int negativeCounter = 0;
            int positiveCounter = 0;

            for (int i = 0; i < data.numInstances(); i++) {
                if (data.instance(i).classValue() == 1.0) {
                    if (positiveCounter < 1776) {
                        subGroup.add(data.instance(i));
                        positiveCounter++;
                    }
                } else {
                    subGroup.add(data.instance(i));
                }
            }
            unfilteredTestData = subGroup;

             */

            testData = unfilteredTestData;

            System.out.println("Test filtered Attributes: " + testData.numAttributes());
            System.out.println("Test Instances: " + testData.numInstances());

        } else {
            throw new Exception();
        }
    }

    public void buildArffTest(boolean nominal) throws Exception {
        Instances data = buildArff("TestTweets", nominal);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        List<TrainingTweet> result = session.createQuery("from TrainingTweet ", TrainingTweet.class).list();
        AtomicInteger counterPositive = new AtomicInteger(0);
        result.forEach(tweet -> {
            if (tweet.getScore() == 0.0) {
                return;
            }

            double[] vals = new double[data.numAttributes()];
            if (nominal) {
                vals[0] = data.attribute(0).indexOfValue(String.valueOf(tweet.getScore()));
            } else {
                vals[0] = tweet.getScore();
            }
            String text = tweet.getText().toLowerCase(Locale.ROOT);
            text = preprocess(text);
            vals[1] = data.attribute(1).addStringValue(text);

            if (isLexiconMethod) {
                double score = lexiconMethod.analyzeTweet(tweet.getText(), false, false);
                if (score < -10) {
                    score = -10;
                } else if (score > 10) {
                    score = 10;
                }
                score += 10;

                vals[2] = score;
            }

            data.add(new DenseInstance(1.0, vals));
            data.setClassIndex(0);
        });
        System.out.println("Testing: " + counterPositive.get() + " positive tweets");
        try {
            if (isLexiconMethod) {
                ConverterUtils.DataSink.write(test_arff_nominal_method3, data);


            } else if (isLexiconMethod2) {
                ConverterUtils.DataSink.write(test_arff_nominal_method32, data);

            } else if (nominal) {
                ConverterUtils.DataSink.write(test_arff_nominal, data);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Instances buildArff(String name, boolean nominal) {
        ArrayList<Attribute> atts;
        if (isLexiconMethod) {
            atts = new ArrayList<>(3);
        } else {
            atts = new ArrayList<>(2);
        }
        if (nominal) {

            List<String> nominalValues = new ArrayList<String>(3);
            nominalValues.add("-1.0");
            if (isLexiconMethod2) {
                //nominalValues.add("0.0");
            }
            nominalValues.add("1.0");
            atts.add(new Attribute("calculatedScore", nominalValues));
        } else {
            atts.add(new Attribute("calculatedScore"));
        }
        atts.add(new Attribute("tweetText", (ArrayList<String>) null));
        if (isLexiconMethod) {
            atts.add(new Attribute("lexiconScore"));
        }
        Instances data = new Instances(name, atts, 0);
        return data;
    }

    private void buildArffTrain(boolean nominal) throws Exception {
        Instances trainData = buildArff("TrainingData", nominal);
        AtomicInteger counter = new AtomicInteger(0);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(training_path), StandardCharsets.UTF_8));
        AtomicInteger counterPositive = new AtomicInteger(0);
        AtomicInteger counterNegative = new AtomicInteger(0);

        reader.lines().forEach(line -> {
            String[] components = line.split("\",\"");
            if (components.length == 6) {
                for (int i = 0; i < components.length; i++) {
                    components[i] = components[i].replaceAll("\"", "");
                }
                Double score = switch (components[0]) {
                    case "0" -> -1.0;
                    //case "\"2\"" -> "0.0";
                    case "4" -> 1.0;
                    default -> null;
                };
                if (score == null) {
                    return;
                }
                String text = components[5];
                double[] vals = new double[trainData.numAttributes()];
                if (nominal) {

                    if (isLexiconMethod2) {
                        double lexiconScore = lexiconMethod.analyzeTweet(text, true, false);
                        if (lexiconScore == 0.0) {
                            return;
                        }
                        vals[0] = trainData.attribute(0).indexOfValue(String.valueOf(lexiconScore));
                    } else if (isLexiconMethod) {
                        double lexiconScore = lexiconMethod.analyzeTweet(text, false, false);

                        if (lexiconScore < -10) {
                            lexiconScore = -10;
                        } else if (lexiconScore > 10.0) {
                            lexiconScore = 10;
                        }
                        lexiconScore += 10;

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
            }
        });
        System.out.println("Instances: " + trainData.numInstances());
        System.out.println("Training: " + counterPositive.get() + " positive tweets");
        System.out.println("Training: " + counterNegative.get() + " negative tweets");

        trainData.setClassIndex(0);
        ConverterUtils.DataSink.write(trainDataPath, trainData);
        trainingData = trainData;

    }


    private void buildInstancesTrain(boolean nominal, int limit) throws Exception {

        ArffLoader loader = new ArffLoader();
        loader.setSource(new File(trainDataPath));
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
            word = removeTripleChar(word);
            if (!filteredWords.contains(word)) {
                sb.append(word).append(" ");
            }
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

    public void caseStudy() throws Exception {
        Instances data = buildArff("CaseStudyTweets", true);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        List<CaseStudyTweet> result = session.createQuery("from CaseStudyTweet", CaseStudyTweet.class).list();
        AtomicInteger counterPositive = new AtomicInteger(0);
        AtomicInteger counterNegative = new AtomicInteger(0);
        FilteredClassifier classifier = (FilteredClassifier) SerializationHelper.read(nb_file);


        for (int i = 0; i < result.size(); i++) {
            Date date = new Date(1652655600000L);
            if (
                    result.get(i).getCreated_at().after(new Date(1652655600000L)) &&
                            Objects.equals(result.get(i).getTopic(), "devin booker")) {
                double[] vals = new double[data.numAttributes()];
                String text = result.get(i).getText().toLowerCase(Locale.ROOT);
                text = preprocess(text);
                vals[1] = data.attribute(1).addStringValue(text);
                data.add(new DenseInstance(1.0, vals));
                data.setClassIndex(0);
                //System.out.println(text);
            }
        }

        for (int i = 0; i < data.numInstances(); i++) {
            try {
                double index = classifier.classifyInstance(data.instance(i));
                String className = data.instance(i).attribute(0).value((int) index);
                if (Objects.equals(className, "1.0")) {

                    counterPositive.incrementAndGet();
                } else {
                    counterNegative.incrementAndGet();
                }
                System.out.println(className + " " + result.get(i).getText());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println(counterPositive.get());
        System.out.println(counterNegative.get());

    }
}
