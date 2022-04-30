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
        MLMethod mlMethod = new MLMethod(sessionFactory, new LexiconMethod(sessionFactory));

        /*
        Correctly Classified Instances        3612               76.4283 %
Incorrectly Classified Instances      1114               23.5717 %
Kappa statistic                          0.4805
Mean absolute error                      0.2794
Root mean squared error                  0.4134
Relative absolute error                 55.5424 %
Root relative squared error             82.1494 %
Total Number of Instances             4726

         */


    }

    public void try2() throws Exception {
        //use lexicon method to train classifier?
        //better version of training data (compared to now: only :) :( )
        MLMethod mlMethod = new MLMethod(sessionFactory, new LexiconMethod(sessionFactory));

        /* Old training instances
        Correctly Classified Instances        3723               78.777  %
Incorrectly Classified Instances      1003               21.223  %
Kappa statistic                          0.4434
Mean absolute error                      0.2451
Root mean squared error                  0.392
Relative absolute error                 57.814  %
Root relative squared error             83.564  %
Total Number of Instances             4726
         */
        /* Old instances but with binary
        Correctly Classified Instances        3806               80.5332 %
Incorrectly Classified Instances       920               19.4668 %
Kappa statistic                          0.5308
Mean absolute error                      0.2502
Root mean squared error                  0.3714
Relative absolute error                 54.1702 %
Root relative squared error             78.5574 %
Total Number of Instances             4726
         */
/* New tweets: https://www.kaggle.com/datasets/thoughtvector/customer-support-on-twitter
        Correctly Classified Instances 3595               76.0686 %
                Incorrectly Classified Instances      1131               23.9314 %
                Kappa statistic                          0.4826
        Mean absolute error                      0.2783
        Root mean squared error                  0.4195
        Relative absolute error                 69.472  %
                Root relative squared error             87.5944 %
                Total Number of Instances             4726

 */

        /* https://www.kaggle.com/datasets/wjia26/twittersentimentbycountry?select=08_2020.csv
        Correctly Classified Instances        3657               77.3804 %
Incorrectly Classified Instances      1069               22.6196 %
Kappa statistic                          0.415
Mean absolute error                      0.2458
Root mean squared error                  0.4189
Relative absolute error                 52.0626 %
Root relative squared error             87.6676 %
Total Number of Instances             4726
         */
    }

    public void try3() {
        //some sort of agreement between different ML models and lexicon method?
    }


}
