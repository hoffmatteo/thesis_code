package com.oth.thesis;

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
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MLMethod {
    private static final String test_arff_nominal = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\testTweetsNominal.arff";
    private static final String test_arff_numeric = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\testTweetsNumeric.arff";
    private static final String nb_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\nbmulti.model";
    private static final String j48_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\j48.model";
    private static final String logistic_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\logistic2.model";
    private static final String svm_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\svm.model";
    private static final String forest_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\forest2.model";
    private static final String linear_file = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\linear.model";
    private static final String training_path = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\data\\training.1600000.processed.noemoticon.csv";
    private static final String dictionary_path = "C:\\Users\\matte\\Desktop\\OTH\\thesis_code\\models\\dictionary.txt";

    private final SessionFactory sessionFactory;
    //TODO better --> differ between nominal here
    private Instances trainingData;
    private Instances testData;
    private Instances unfilteredTestData;
    StringToWordVector filter = new StringToWordVector();


    public MLMethod(SessionFactory sessionFactory) throws Exception {
        this.sessionFactory = sessionFactory;
        filter.setLowerCaseTokens(true);
        filter.setDoNotOperateOnPerClassBasis(true);
        filter.setOptions(new String[]{"-W", "15000", "-M", "10", "-dictionary", dictionary_path, "-L", "-C"});

        buildInstancesTrain(true);
        buildArffTest(true);
        buildInstancesTest(true);


        runNaiveBayes();
        //runLogisticRegression();
        //runLinearRegression();
        //runRandomForest();
        //runSVM();
        //runJ48();
    }

    public void runNaiveBayes() throws Exception {
        boolean nominal = true;
        //buildInstancesTrain(nominal);
        //buildArffTest(nominal);
        //buildInstancesTest(nominal);
        //NaiveBayes nb = new NaiveBayes();
        NaiveBayesMultinomial nb = new NaiveBayesMultinomial();

        train(nb, nb_file);
        test(nb_file, nominal);
    }

    public void runLogisticRegression() throws Exception {
        boolean nominal = true;
        buildInstancesTrain(nominal);
        //buildArffTest(nominal);
        buildInstancesTest(nominal);
        Logistic log = new Logistic();
        log.setOptions(new String[]{"-S", "-M", "10"});

        train(log, logistic_file);
        test(logistic_file, nominal);

    }

    public void runSVM() throws Exception {
        boolean nominal = true;
        //buildInstancesTrain(nominal);
        //buildArffTest(nominal);
        //buildInstancesTest(nominal);
        LibSVM svm = new LibSVM();
        //linear
        svm.setOptions(new String[]{"-K", "0"});

        train(svm, svm_file);
        test(svm_file, nominal);

    }

    public void runRandomForest() throws Exception {
        boolean nominal = true;
        buildInstancesTrain(nominal);
        //buildArffTest(nominal);
        buildInstancesTest(nominal);
        RandomForest forest = new RandomForest();
        forest.setOptions(new String[]{"-depth", "300", "-print", ""});
        train(forest, forest_file);
        test(forest_file, nominal);

    }

    public void runLinearRegression() throws Exception {
        boolean nominal = false;
        buildInstancesTrain(nominal);
        //buildArffTest(nominal);
        buildInstancesTest(nominal);
        //train(new LinearRegression(), linear_file);
        test(linear_file, nominal);

    }

    public void runJ48() throws Exception {
        boolean nominal = true;
        buildInstancesTrain(nominal);
        //buildArffTest(nominal);
        buildInstancesTest(nominal);
        //train(new NaiveBayes(), j48_file);
        test(j48_file, nominal);
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

    public void evaluate(Classifier classifier, boolean nominal) throws Exception {
        Evaluation eval = new Evaluation(trainingData);
        System.out.println("Training: " + trainingData.numAttributes());
        System.out.println("Testing: " + testData.numAttributes());

        eval.evaluateModel(classifier, testData);

        //System.out.println("** " + classifier.getClassifier().getClass() + " Evaluation with Datasets **");
        System.out.println(eval.toSummaryString());
        //System.out.println(classifier);
        for (int i = 0; i < 50; i++) {
            //System.out.println(testData.instance(i));
            System.out.println(unfilteredTestData.instance(i));

            double result = classifier.classifyInstance(testData.instance(i));
            //System.out.println("Index: " + index);
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

    }

    public void buildInstancesTest(boolean nominal) throws Exception {
        File f;
        if (nominal) {
            f = new File(test_arff_nominal);
        } else {
            f = new File(test_arff_numeric);
        }
        if (f.exists()) {
            ArffLoader loader = new ArffLoader();
            loader.setSource(f);
            Instances data = loader.getDataSet();
            data.setClassIndex(0);
            unfilteredTestData = data;

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
            if (tweet.getScore() == 1.0) {
                if (counterPositive.get() > 830) {
                    //return;
                } else {
                    counterPositive.incrementAndGet();
                }
            }
            double[] vals = new double[data.numAttributes()];
            if (nominal) {
                vals[0] = data.attribute(0).indexOfValue(String.valueOf(tweet.getScore()));
            } else {
                vals[0] = tweet.getScore();
            }
            vals[1] = data.attribute(1).addStringValue(tweet.getText());

            data.add(new DenseInstance(1.0, vals));
            data.setClassIndex(0);
        });
        System.out.println("Testing: " + counterPositive.get() + " positive tweets");
        try {
            if (nominal) {
                ConverterUtils.DataSink.write(test_arff_nominal, data);
            } else {
                ConverterUtils.DataSink.write(test_arff_numeric, data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Instances buildArff(String name, boolean nominal) {
        ArrayList<Attribute> atts = new ArrayList<>(2);
        if (nominal) {
            List<String> nominalValues = new ArrayList<String>(3);
            nominalValues.add("-1.0");
            //nominalValues.add("0.0");
            nominalValues.add("1.0");
            atts.add(new Attribute("calculatedScore", nominalValues));
        } else {
            atts.add(new Attribute("calculatedScore"));
        }
        atts.add(new Attribute("tweetText", (ArrayList<String>) null));
        Instances data = new Instances(name, atts, 0);
        return data;
    }


    private void buildInstancesTrain(boolean nominal) throws Exception {
        Instances trainData = buildArff("TrainingData", nominal);
        AtomicInteger counter = new AtomicInteger(0);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(training_path), StandardCharsets.UTF_8));
        AtomicInteger counterPositive = new AtomicInteger(0);
        AtomicInteger counterNegative = new AtomicInteger(0);

        reader.lines().forEach(line -> {
            if (counter.incrementAndGet() % 10 != 0) {
                //return;
            }
            String[] components = line.split(",");
            if (components.length == 6) {
                Double score = switch (components[0]) {
                    case "\"0\"" -> -1.0;
                    //case "\"2\"" -> "0.0";
                    case "\"4\"" -> 1.0;
                    default -> null;
                };
                if (score == null) {
                    return;
                }
                String text = components[5];
                double[] vals = new double[trainData.numAttributes()];
                if (nominal) {
                    vals[0] = trainData.attribute(0).indexOfValue(String.valueOf(score));
                } else {
                    vals[0] = score;
                }

                text = text.replaceAll("\"", "");
                vals[1] = trainData.attribute(1).addStringValue(text);

                trainData.add(new DenseInstance(1.0, vals));
                trainData.setClassIndex(0);

            }
        });
        System.out.println("Instances: " + trainData.numInstances());
        System.out.println("Training: " + counterPositive.get() + " positive tweets");
        System.out.println("Training: " + counterNegative.get() + " negative tweets");

        trainData.setClassIndex(0);
        trainingData = trainData;
    }
}
