import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
//import weka.core.*;
//import weka.classifiers.functions.LinearRegression;
import weka.classifiers.evaluation.Evaluation;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.trees.RandomForest;


public class extraCredit {
//    private LinearRegression model;
    private RandomForest model;

    public static double removedPercentage = 35.0;
    public static class Pair{
        int x;
        int y;
        public Pair(int a, int b){
            this.x = a;
            this.y = b;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair pair = (Pair) o;
            return x == pair.x && y == pair.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    public static class ImageFeatures{
        double entropy;
        double edgeDensity;
        double colorVariance;
        double percentageLost;
        double reconstructionError;

        public ImageFeatures(double a , double b, double c,double d, double e){
            this.entropy = a;
            this.edgeDensity = b;
            this.colorVariance = c;
            this.percentageLost = d;
            this.reconstructionError = e;
        }

    }
    int width = 1920;
    int height = 1080;
    BufferedImage originalImg;
    HashSet<Pair> removedPixelsSet;
    HashMap<String, ImageFeatures> imageFeaturesHashMap= new HashMap<>();

    public void saveImage(BufferedImage img, String outputPath) {
        try {
            File outputfile = new File(outputPath);
            ImageIO.write(img, "jpg", outputfile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedImage createDeepCopy(BufferedImage source) {
        ColorModel cm = source.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = source.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }
    public BufferedImage removeSamples(BufferedImage img, double percentage) {
        int totalPixels = width * height;
        int pixelsToRemove = (int) ((percentage * totalPixels)/100);
        int removedPixels = 0;
        removedPixelsSet = new HashSet<Pair>();

        while(removedPixels < pixelsToRemove){
            int x = (int) (Math.random() * width);
            int y = (int) (Math.random() * height);
            Pair p = new Pair(x,y);
            if(!removedPixelsSet.contains(p)){
                int black = new Color(0, 0, 0).getRGB();
                img.setRGB(x, y, black);
                removedPixelsSet.add(p);
                removedPixels++;
            }
        }
        return img;
    }
    public BufferedImage reconstructImageNewest(BufferedImage img) {

        BufferedImage resultImg = createDeepCopy(img);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // check if the pixel is missing
                if(removedPixelsSet.contains(new Pair(x, y))){
                    int sumRed = 0, sumGreen = 0, sumBlue = 0, goodPixelscount = 0;
                    for (int ky = -1; ky <= 1; ky++) {
                        for (int kx = -1; kx <= 1; kx++) {
                            int nx = x + kx;
                            int ny = y + ky;
                            if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                                int neighborColor = resultImg.getRGB(nx, ny);
                                //Check if pixel is missing
                                if(!removedPixelsSet.contains(new Pair(nx, ny))){
                                    sumRed += (neighborColor >> 16) & 0xff;
                                    sumGreen += (neighborColor >> 8) & 0xff;
                                    sumBlue += neighborColor & 0xff;
                                    goodPixelscount++;
                                }
                            }
                        }
                    }
                    if (goodPixelscount > 0) {
                        //Take average of the neighbors
                        int avgR = sumRed / goodPixelscount;
                        int avgG = sumGreen / goodPixelscount;
                        int avgB = sumBlue / goodPixelscount;
                        int avgColor = (255 << 24) | (avgR << 16) | (avgG << 8) | avgB;
                        resultImg.setRGB(x, y, avgColor);
                    } else{
                        //If no neighboring pixels are valid
                        resultImg.setRGB(x, y, Color.BLACK.getRGB());
                    }
                }
            }
        }
        return resultImg;
    }
    public double computeReconstructionErrorPixelWise(BufferedImage reconstructed) {
        double squaredSumOfErrors = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int originalColor = originalImg.getRGB(x, y);
                int reconstructedColor = reconstructed.getRGB(x, y);
                double diff = Math.abs(originalColor - reconstructedColor);
                squaredSumOfErrors += diff * diff;
            }
        }
        return squaredSumOfErrors;
    }

    public ArrayList<Double> processImage(String imgPath) throws IOException {
        ArrayList<Double> errorList = new ArrayList<>();

        originalImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        readImageRGB(width, height, imgPath, originalImg);
        int rp = (int)removedPercentage;

        for (int j = rp; j <= rp; j++) {
            double i = (j / 100.0);

            BufferedImage removedPixelImage = createDeepCopy(originalImg);
            removedPixelImage = removeSamples(removedPixelImage, j);
            String outputPathRemoved = "/Users/vee/Desktop/Removedpixel_Images/removedPixel_" + j + "%.jpg";
            saveImage(removedPixelImage, outputPathRemoved);

            BufferedImage reconstructed = reconstructImageNewest(removedPixelImage);
            double error = computeReconstructionErrorPixelWise(reconstructed);
            errorList.add(error);

            String outputPathReconstructed = "/Users/vee/Desktop/Reconstructed_Images/reconstructed_" + j + "%.jpg";
            saveImage(reconstructed, outputPathReconstructed);

        }

        return errorList;
    }
    private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
    {
        try
        {
            int frameLength = width*height*3;

            File file = new File(imgPath);
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);

            long len = frameLength;
            byte[] bytes = new byte[(int) len];

            raf.read(bytes);

            int ind = 0;
            for(int y = 0; y < height; y++)
            {
                for(int x = 0; x < width; x++)
                {
                    byte a = 0;
                    byte r = bytes[ind];
                    byte g = bytes[ind+height*width];
                    byte b = bytes[ind+height*width*2];

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    img.setRGB(x,y,pix);
                    ind++;
                }
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    public static double computeEntropy(BufferedImage img) {
        int[] histogram = new int[256];
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int color = img.getRGB(x, y);
                int red = (color >> 16) & 0xff;
                int green = (color >> 8) & 0xff;
                int blue = color & 0xff;
                histogram[red]++;
                histogram[green]++;
                histogram[blue]++;
            }
        }

        double entropy = 0;
        for (int i = 0; i < 256; i++) {
            if (histogram[i] > 0) {
                double probability = (double) histogram[i] / (img.getWidth() * img.getHeight() * 3);
                entropy -= probability * (Math.log(probability) / Math.log(2));
            }
        }

        return entropy;
    }

    public static double computeEdgeDensity(BufferedImage img) {
        int edges = 0;
        for (int x = 1; x < img.getWidth() - 1; x++) {
            for (int y = 1; y < img.getHeight() - 1; y++) {
                int color = img.getRGB(x, y);
                int colorPrevX = img.getRGB(x-1, y);
                int colorNextX = img.getRGB(x+1, y);
                int colorPrevY = img.getRGB(x, y-1);
                int colorNextY = img.getRGB(x, y+1);

                double gradientX = Math.abs(color - colorPrevX) + Math.abs(color - colorNextX);
                double gradientY = Math.abs(color - colorPrevY) + Math.abs(color - colorNextY);

                if (gradientX > 25 || gradientY > 25) {
                    edges++;
                }
            }
        }

        return (double) edges / (img.getWidth() * img.getHeight());
    }

    public static double computeColorVariance(BufferedImage img) {
        double sumR = 0, sumG = 0, sumB = 0;
        double sumR2 = 0, sumG2 = 0, sumB2 = 0;
        int numPixels = img.getWidth() * img.getHeight();

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int color = img.getRGB(x, y);
                int red = (color >> 16) & 0xff;
                int green = (color >> 8) & 0xff;
                int blue = color & 0xff;

                sumR += red;
                sumG += green;
                sumB += blue;

                sumR2 += red * red;
                sumG2 += green * green;
                sumB2 += blue * blue;
            }
        }

        double meanR = sumR / numPixels;
        double meanG = sumG / numPixels;
        double meanB = sumB / numPixels;

        double varianceR = (sumR2 / numPixels) - (meanR * meanR);
        double varianceG = (sumG2 / numPixels) - (meanG * meanG);
        double varianceB = (sumB2 / numPixels) - (meanB * meanB);

        return (varianceR + varianceG + varianceB) / 3.0;
    }

    public double computeReconstructionError(String imgPath) throws IOException {
            ArrayList<Double> errors = processImage(imgPath);
            return errors.get(0);
    }

    private double[] coefficients;

    public void train(HashMap<String, ImageFeatures> dataMap) {
        int n = dataMap.size();
        double sumX1 = 0, sumX2 = 0, sumX3 = 0, sumY = 0;
        double sumX1X1 = 0, sumX2X2 = 0, sumX3X3 = 0;
        double sumX1Y = 0, sumX2Y = 0, sumX3Y = 0;

        for (ImageFeatures features : dataMap.values()) {
            sumX1 += features.entropy;
            sumX2 += features.edgeDensity;
            sumX3 += features.colorVariance;
            sumY += features.reconstructionError;

            sumX1X1 += features.entropy * features.entropy;
            sumX2X2 += features.edgeDensity * features.edgeDensity;
            sumX3X3 += features.colorVariance * features.colorVariance;

            sumX1Y += features.entropy * features.reconstructionError;
            sumX2Y += features.edgeDensity * features.reconstructionError;
            sumX3Y += features.colorVariance * features.reconstructionError;
        }

        double avgX1 = sumX1 / n;
        double avgX2 = sumX2 / n;
        double avgX3 = sumX3 / n;
        double avgY = sumY / n;

        double covX1Y = sumX1Y / n - avgX1 * avgY;
        double covX2Y = sumX2Y / n - avgX2 * avgY;
        double covX3Y = sumX3Y / n - avgX3 * avgY;

        double varX1 = sumX1X1 / n - avgX1 * avgX1;
        double varX2 = sumX2X2 / n - avgX2 * avgX2;
        double varX3 = sumX3X3 / n - avgX3 * avgX3;

        double b1 = covX1Y / varX1;
        double b2 = covX2Y / varX2;
        double b3 = covX3Y / varX3;

        double b0 = avgY - b1 * avgX1 - b2 * avgX2 - b3 * avgX3;

        coefficients = new double[]{b0, b1, b2, b3};
//        return coefficients;
    }

    public double predict(ImageFeatures features) {
        double predictedValue = coefficients[0] + coefficients[1] * features.entropy + coefficients[2] * features.edgeDensity + coefficients[3] * features.colorVariance;

        double threshold = 2100.0;
        if(features.colorVariance < threshold) {
            double reductionFactor = 0.7 + 0.3 * (features.colorVariance / threshold);
            predictedValue *= reductionFactor;
        }

        return predictedValue;
    }

    public static double[] normalizeFeatureValues(HashMap<String, ImageFeatures> dataMap) {
        double sumEntropy = 0, sumEdgeDensity = 0, sumColorVariance = 0;
        int n = dataMap.size();

        for (ImageFeatures features : dataMap.values()) {
            sumEntropy += features.entropy;
            sumEdgeDensity += features.edgeDensity;
            sumColorVariance += features.colorVariance;
        }

        double avgEntropy = sumEntropy / n;
        double avgEdgeDensity = sumEdgeDensity / n;
        double avgColorVariance = sumColorVariance / n;

        double varianceEntropy = 0, varianceEdgeDensity = 0, varianceColorVariance = 0;

        for (ImageFeatures features : dataMap.values()) {
            varianceEntropy += Math.pow(features.entropy - avgEntropy, 2);
            varianceEdgeDensity += Math.pow(features.edgeDensity - avgEdgeDensity, 2);
            varianceColorVariance += Math.pow(features.colorVariance - avgColorVariance, 2);
        }

        varianceEntropy /= n;
        varianceEdgeDensity /= n;
        varianceColorVariance /= n;

        double stdDevEntropy = Math.sqrt(varianceEntropy);
        double stdDevEdgeDensity = Math.sqrt(varianceEdgeDensity);
        double stdDevColorVariance = Math.sqrt(varianceColorVariance);

        // Normalize the feature values
        for (String key : dataMap.keySet()) {
            ImageFeatures features = dataMap.get(key);
            features.entropy = (features.entropy - avgEntropy) / stdDevEntropy;
            features.edgeDensity = (features.edgeDensity - avgEdgeDensity) / stdDevEdgeDensity;
            features.colorVariance = (features.colorVariance - avgColorVariance) / stdDevColorVariance;
        }

        return new double[]{avgEntropy, stdDevEntropy, avgEdgeDensity, stdDevEdgeDensity, avgColorVariance, stdDevColorVariance};
    }

    private Instances convertToWekaInstances(HashMap<String, ImageFeatures> dataMap) {
        ArrayList<Attribute> attributes = new ArrayList<>();

        // Defining attributes for Weka
        attributes.add(new Attribute("entropy"));
        attributes.add(new Attribute("edgeDensity"));
        attributes.add(new Attribute("colorVariance"));
        attributes.add(new Attribute("percentageLost"));
        attributes.add(new Attribute("reconstructionError"));

        Instances dataset = new Instances("ImageData", attributes, dataMap.size());
        dataset.setClassIndex(dataset.numAttributes() - 1);

        for (ImageFeatures features : dataMap.values()) {
            double[] values = new double[]{
                    features.entropy,
                    features.edgeDensity,
                    features.colorVariance,
                    features.percentageLost,
                    features.reconstructionError
            };

            dataset.add(new DenseInstance(1.0, values));
        }

        return dataset;
    }

    private Instance convertToWekaInstance(ImageFeatures features, Instances datasetStructure) {
        double[] values = new double[]{
                features.entropy,
                features.edgeDensity,
                features.colorVariance,
                features.percentageLost,
                features.reconstructionError
        };

        Instance instance = new DenseInstance(1.0, values);
        instance.setDataset(datasetStructure);
        return instance;
    }

    public void train2(HashMap<String, ImageFeatures> dataMap) {
        Instances trainingSet = convertToWekaInstances(dataMap);
        model = new RandomForest();
        model.setNumIterations(100);
        model.setMaxDepth(10);


        try {
            model.buildClassifier(trainingSet);
            Evaluation eval = new Evaluation(trainingSet);
            eval.crossValidateModel(model, trainingSet, 5, new Random(1));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double predict2(ImageFeatures features) {
        Instances dataSet = convertToWekaInstances(imageFeaturesHashMap);
        Instance instanceToPredict = convertToWekaInstance(features, dataSet);
        double predictedValue = Double.NaN;

        try {
            predictedValue = model.classifyInstance(instanceToPredict);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return predictedValue;
    }

    public static void main(String[] args) throws IOException {
        extraCredit e = new extraCredit();

        String testImagePath = args[0];
        double removedPercentageInput = Double.parseDouble(args[1]);
        ImageFeatures testImageObject = new ImageFeatures(0.0,0.0,0.0,0.0,0.0);

        // List of image paths
        String[] imagePaths = {
                "./lake-forest.rgb",
                "./miamibeach.rgb",
                "./mountain.rgb",
                "./stagforest.rgb",
                "./worldmap.rgb",
                "./skyclouds.rgb",
                testImagePath
        };
//        removedPercentage = removedPercentageInput;
//        removedPercentage = 47;

         e.originalImg = new BufferedImage(e.width, e.height, BufferedImage.TYPE_INT_RGB);

         for(String imgPath: imagePaths){
             String pictureLabel = imgPath.substring(2);
             System.out.println("Picture - "+ imgPath.substring(2));
             e.readImageRGB(e.width, e.height, imgPath, e.originalImg );
             System.out.println("Entropy: " + computeEntropy(e.originalImg ));
             System.out.println("Edge Density: " + computeEdgeDensity(e.originalImg ));
             System.out.println("Color Variance: " + computeColorVariance(e.originalImg ));

             double entropy = computeEntropy(e.originalImg );
             double edgeDensity = computeEdgeDensity(e.originalImg );
             double colorVariance = computeColorVariance(e.originalImg );
             double reconstructionError = e.computeReconstructionError(imgPath);

             System.out.println("Reconstruction Error: " + reconstructionError);
             System.out.println("***********");
             ImageFeatures i = new ImageFeatures(entropy, edgeDensity, colorVariance,removedPercentage, reconstructionError);

             if(imgPath != testImagePath){
                 e.imageFeaturesHashMap.put(pictureLabel,i);
             }
             else{
                 testImageObject = i;
             }

         }
         System.out.println("HASHMAP");
         for(String x: e.imageFeaturesHashMap.keySet()){
             ImageFeatures ifeatures = e.imageFeaturesHashMap.get(x);
             System.out.println(x+" "+ ifeatures.entropy+" "+ ifeatures.edgeDensity+" "+ ifeatures.colorVariance+" "+ ifeatures.reconstructionError);
         }

        e.train2(e.imageFeaturesHashMap);
        double avgError = 0.0;

        for (String imageName : e.imageFeaturesHashMap.keySet()) {
            ImageFeatures features = e.imageFeaturesHashMap.get(imageName);
            double predictedError = e.predict2(features);
            double actualError = features.reconstructionError;
            double percentageError = 0;

            if (actualError != 0) {
                percentageError = Math.abs((predictedError - actualError) / actualError) * 100;
                avgError += percentageError;
            }

        }
        avgError = avgError/imagePaths.length;
        System.out.println("---------------------------------------------");
        System.out.println("Total Mean Absolute Percentage Error in prediction : " + String.format("%.2f", avgError)+"%");
        System.out.println("---------------------------------------------");

//        ImageFeatures featuresInput = e.imageFeaturesHashMap.get("testImagePath");
        testImageObject.percentageLost = removedPercentageInput;
        double predictedErrorInput = e.predict2(testImageObject);
        double actualError = testImageObject.reconstructionError;


        if (actualError != 0) {
            double errorPercentageforInputImage = Math.abs((predictedErrorInput - actualError) / actualError) * 100;
            System.out.println("Predicted error for input image : " + predictedErrorInput);
            System.out.println("Actual error for input image : " + actualError);
            System.out.println("Percentage of error in prediction for input image :" + String.format("%.2f", errorPercentageforInputImage)+"%");
        }
    }
}
