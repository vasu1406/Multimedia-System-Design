# The images are in the same file and image paths are given in the code accordingly. The java file and images should be in the same
location for the code to work. I have added the same images in the folder as in 1920X1080_data_samples.
# Added jar files also in the same folder

# The Random Forest is used in this algorithm to predict the reconstruction error of images based on extracted features
such as entropy, edge density, and color variance, by training on a set of sample images and their computed features.

## First we need to compile the file using the command (use your path for the weka jar, can be downloaded from here - https://sourceforge.net/projects/weka/ )

javac -cp .:/Users/vee/Downloads/weka-3-8-6/weka.jar extraCredit.java

## Then run the below command to run the file

java -cp .:/Users/vee/Downloads/weka-3-8-6/weka.jar extraCredit "removePercentage"
For e.g - java -cp .:/Users/vee/Downloads/weka-3-8-6/weka.jar extraCredit 25

## File prints

1) Entropy, Edge Density, color variance, percentage of removed pixel and actual reconstruction error for each image
2) At the end it print the "Total Mean Absolute Percentage Error" in prediction of the model.

