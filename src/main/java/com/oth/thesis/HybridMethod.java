package com.oth.thesis;

import org.hibernate.SessionFactory;

public class HybridMethod {
    //general: try out each method, test and evaluate results
    //better than method1 and method2, which one is best?

    private LexiconMethod lexiconMethod;
    private SessionFactory sessionFactory;

    public HybridMethod(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }


    public void try1() throws Exception {
        //run training instances through lexicon method first
        //score of lexicon method --> feature for classifier
        MLMethod mlMethod = new MLMethod(sessionFactory, new LexiconMethod(sessionFactory), false);


    }

    public void try2() throws Exception {
        //use lexicon method to train classifier
        //better version of training data (compared to now: only :) :( )
        MLMethod mlMethod = new MLMethod(sessionFactory, new LexiconMethod(sessionFactory), true);

    }
}
