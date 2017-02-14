package hbvOptimization;


import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.NumberedFileInputSplit;
import org.datavec.api.util.ClassPathResource;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.deeplearning4j.eval.RegressionEvaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DischargeForecast {
    private static final Logger LOGGER = LoggerFactory.getLogger(DischargeForecast.class);

    private static File baseDir = new File("/disk1/project/NIE");
    private static File baseTrainDir = new File(baseDir, "multiTimestepTrain");
    private static File featuresDirTrain = new File(baseTrainDir, "features");
    private static File labelsDirTrain = new File(baseTrainDir, "labels");
    private static File baseTestDir = new File(baseDir, "multiTimestepTest");
    private static File featuresDirTest = new File(baseTestDir, "features");
    private static File labelsDirTest = new File(baseTestDir, "labels");

    static int numInputs = 17;
    static int numOutputs = 1;
    static int seed=60;
    static int iterations=1;
    static int numHiddenNodes = 20;
    static double learningRate = 0.01;
    
    public static void main(String[] args) throws Exception {

    	//First: get the dataset using the record reader. CSVRecordReader handles loading/parsing
        int numLinesToSkip = 0;
        String delimiter = ",";
        RecordReader recordReader = new CSVRecordReader(numLinesToSkip,delimiter);
        recordReader.initialize(new FileSplit(new File("/disk1/project/NIE/processedData.csv")));
        
        //Second: the RecordReaderDataSetIterator handles conversion to DataSet objects, ready for use in neural network
        int labelIndex = 17;     //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
        int batchSize = 7000;    //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)

        DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader,batchSize,labelIndex, labelIndex, true);
        DataSet allData = iterator.next();
        System.out.println("allData size: "+allData.numExamples());
        System.out.println("before normalizer data \r\n"+allData.getRange(0,9));
        
        NormalizerMinMaxScaler normalizer = new NormalizerMinMaxScaler(0,1);
        normalizer.fitLabel(true);
        normalizer.fit(allData);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data
        normalizer.transform(allData);     //Apply normalization to the training data
        
        SplitTestAndTrain testAndTrain = allData.splitTestAndTrain(0.82);  //Use 65% of data for training

        DataSet trainingData = testAndTrain.getTrain();
        int ts=trainingData.numExamples();
        DataSet testData = testAndTrain.getTest();
        
        //We need to normalize our data. We'll use NormalizeStandardize (which gives us mean 0, unit variance):
        System.out.println("after normalizer train data\r\n"+trainingData.getRange(0,9));
        System.out.println("after normalizer test data\r\n"+testData.getRange(0,9));

        // ----- Configure the network -----
        MultiLayerConfiguration conf = getRNNNetworkConfiguration();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        //net.setListeners(new ScoreIterationListener(20));

        // ----- Train the network, evaluating the test set performance at each epoch -----
        int nEpochs = 10;

        for( int i=0; i<nEpochs; i++ ){
	    	net.fit(trainingData);
	    	
	    	LOGGER.info("Epoch " + i + " complete. Time series evaluation:");
	    	// Run regression evaluation on our single column input
	    	RegressionEvaluation evaluation = new RegressionEvaluation(1);
	    	INDArray features = testData.getFeatureMatrix();
	    	INDArray predicted = net.output(features, false).getColumn(0);
	    	INDArray lables = testData.getLabels();
	    	
	    	evaluation.eval(lables, predicted);
	    	System.out.println(evaluation.stats());
	    	System.out.println("Nash–Sutcliffe efficiency coefficient: "+nashEvaluation(lables, predicted));
	    }

        /**
         * All code below this point is only necessary for plotting
         */

        //Init rrnTimeStemp with train data and predict test data
        //net.rnnTimeStep(trainingData.getFeatureMatrix());

        //INDArray predicted  = net.rnnTimeStep(testData.getFeatureMatrix());
        INDArray predicted = net.output(testData.getFeatureMatrix());
        normalizer.revertLabels(predicted);

        //Convert raw string data to IndArrays for plotting
        INDArray trainArray = trainingData.getLabels().getColumn(0);
        INDArray testArray = testData.getLabels().getColumn(0);
        normalizer.revertLabels(trainArray);
        normalizer.revertLabels(testArray);

        int trainSize=trainingData.numExamples();
        //Create plot with out data
        XYSeriesCollection c = new XYSeriesCollection();
        createSeries(c, trainArray, 0, "Train data");
        createSeries(c, testArray, trainSize-1, "Actual test data");
        createSeries(c, predicted, trainSize-1, "Predicted test data");

        plotDataset(c);

        LOGGER.info("MultiLayerConfiguration seed:"+conf.getConf(0).getSeed());
        LOGGER.info("----- Example Complete -----");
    }

    private static double nashEvaluation(INDArray labels, INDArray predictions) {
        double errorLP=0;
        double errorLM=0;
        double meanObs=0;
        for (int i=0;i<labels.length();i++) {
            errorLP+=Math.pow(labels.getDouble(i)-predictions.getDouble(i),2);
            meanObs+=labels.getDouble(i);
        }
        meanObs=meanObs/labels.length();
        for (int i=0;i<labels.length();i++) {
            errorLM+=Math.pow(labels.getDouble(i)-meanObs,2);
        }
        return 1-errorLP/errorLM;
    }
    
    private static MultiLayerConfiguration getDeepDenseLayerNetworkConfiguration() {
    	
        return new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(iterations)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(learningRate)
                .weightInit(WeightInit.XAVIER)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .list()
                .layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes)
                        .activation(Activation.TANH).build())
                .layer(1, new DenseLayer.Builder().nIn(numHiddenNodes).nOut(numHiddenNodes)
                        .activation(Activation.TANH).build())
                .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nIn(numHiddenNodes).nOut(numOutputs).build())
                .pretrain(false).backprop(true).build();
    }
    
    private static MultiLayerConfiguration getRNNNetworkConfiguration() {
    	return new NeuralNetConfiguration.Builder()
                //.seed(seed)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .iterations(iterations)
                .weightInit(WeightInit.XAVIER)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .learningRate(0.15)
                .list()
                .layer(0, new GravesLSTM.Builder().activation(Activation.TANH).nIn(numInputs).nOut(10)
                    .build())
                .layer(1, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                    .activation(Activation.RELU).nIn(10).nOut(numOutputs).build())
                .build();
    }

    /**
     * Creates an IndArray from a list of strings
     * Used for plotting purposes
     */
    private static INDArray createIndArrayFromStringList(List<String> rawStrings, int startIndex, int length) {
        List<String> stringList = rawStrings.subList(startIndex,startIndex+length);
        double[] primitives = new double[stringList.size()];

        for (int i = 0; i < stringList.size(); i++) {
            primitives[i] = Double.valueOf(stringList.get(i));
        }

        return Nd4j.create(new int[]{1,length},primitives);
    }

    /**
     * Used to create the different time series for ploting purposes
     */
    private static XYSeriesCollection createSeries(XYSeriesCollection seriesCollection, INDArray data, int offset, String name) {
        int nRows = data.shape()[0];
        XYSeries series = new XYSeries(name);
        for (int i = 0; i < nRows; i++) {
            series.add(i + offset, data.getDouble(i));
        }

        seriesCollection.addSeries(series);

        return seriesCollection;
    }

    /**
     * Generate an xy plot of the datasets provided.
     */
    private static void plotDataset(XYSeriesCollection c) {

        String title = "Regression example";
        String xAxisLabel = "Timestep";
        String yAxisLabel = "Number of passengers";
        PlotOrientation orientation = PlotOrientation.VERTICAL;
        boolean legend = true;
        boolean tooltips = false;
        boolean urls = false;
        JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, c, orientation, legend, tooltips, urls);

        // get a reference to the plot for further customisation...
        final XYPlot plot = chart.getXYPlot();

        // Auto zoom to fit time series in initial window
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRange(true);

        JPanel panel = new ChartPanel(chart);

        JFrame f = new JFrame();
        f.add(panel);
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.pack();
        f.setTitle("Training Data");

        RefineryUtilities.centerFrameOnScreen(f);
        f.setVisible(true);
    }

    /**
     * This method shows how you based on a text file can preprocess your data the structure expected for a
     * multi time step problem. This examples uses a single column CSV as input, but the example should be easy to modify
     * for use with a multi column input as well.
     * @return
     * @throws IOException
     */
    private static List<String> prepareTrainAndTest(int trainSize, int testSize, int numberOfTimesteps) throws IOException {
        Path rawPath = Paths.get(baseDir.getAbsolutePath() + "/data.csv");

        List<String> rawStrings = Files.readAllLines(rawPath, Charset.defaultCharset());

        //Remove all files before generating new ones
        FileUtils.cleanDirectory(featuresDirTrain);
        FileUtils.cleanDirectory(labelsDirTrain);
        FileUtils.cleanDirectory(featuresDirTest);
        FileUtils.cleanDirectory(labelsDirTest);

        for (int i = 0; i < trainSize; i++) {
            Path featuresPath = Paths.get(featuresDirTrain.getAbsolutePath() + "/train_" + i + ".csv");
            Path labelsPath = Paths.get(labelsDirTrain + "/train_" + i + ".csv");
            int j;
            for (j = 0; j < numberOfTimesteps; j++) {
            	Files.write(featuresPath,rawStrings.get(i+j).concat(System.lineSeparator()).getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            }
            Files.write(labelsPath,rawStrings.get(i+j-1).concat(System.lineSeparator()).getBytes(),StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        }

        for (int i = trainSize; i < testSize+trainSize; i++) {
            Path featuresPath = Paths.get(featuresDirTest + "/test_" + i + ".csv");
            Path labelsPath = Paths.get(labelsDirTest + "/test_" + i + ".csv");
            int j;
            for (j = 0; j < numberOfTimesteps; j++) {
                Files.write(featuresPath,rawStrings.get(i+j).concat(System.lineSeparator()).getBytes(),StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            }
            Files.write(labelsPath,rawStrings.get(i+j-1).concat(System.lineSeparator()).getBytes(),StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        }

        return rawStrings;
    }
}
