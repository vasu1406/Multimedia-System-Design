
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.concurrent.TimeUnit;
import javax.swing.*;


public class NewImageDisplay {

    JFrame frame;
    JLabel lbIm1;
    BufferedImage imgOne;
    int width = 512;
    int height = 512;
    private void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
        try {
            int frameLength = width * height * 3;

            File file = new File(imgPath);
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);

            long len = frameLength;
            byte[] bytes = new byte[(int) len];

            raf.read(bytes);

            int ind = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    byte a = 0;
                    byte r = bytes[ind];
                    byte g = bytes[ind + height * width];
                    byte b = bytes[ind + height * width * 2];

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    img.setRGB(x, y, pix);
                    ind++;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void showIms(String[] args){

        String param1 = args[1];
        System.out.println("The second parameter was: " + param1);

        imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        readImageRGB(width, height, args[0], imgOne);

        applyDWT(imgOne, Integer.parseInt(param1));
    }
    private int[][] makeInputArrayForRedChannel(BufferedImage img){
        int[][] result = new int[height][width];
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x <  512; x++) {
                int pixel1 = img.getRGB(x, y);
                int red1 = (pixel1 >> 16) & 0xFF;
                result[y][x] = red1;
            }
        }
        return result;
    }
    private int[][] makeInputArrayForGreenChannel(BufferedImage img){
        int[][] result = new int[height][width];
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x <  512; x++) {
                int pixel1 = img.getRGB(x,y);
                int green1 = (pixel1 >> 8) & 0xFF;

                result[y][x] = green1;
            }
        }
        return result;
    }
    private int[][] makeInputArrayForBlueChannel(BufferedImage img){
        int[][] result = new int[height][width];
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x <  512; x++) {
                int pixel1 = img.getRGB(x,y);
                int blue1 = pixel1 & 0xFF;
                result[y][x] = blue1;
            }
        }
        return result;
    }
    private void applyDWT(BufferedImage img, int n) {
        int[][] inputArrayForRed = makeInputArrayForRedChannel(img);
        int[][] inputArrayForGreen = makeInputArrayForGreenChannel(img);
        int[][] inputArrayForBlue = makeInputArrayForBlueChannel(img);

        int[][] redResult = applyDWTToChannel(inputArrayForRed);
        int[][] greenResult = applyDWTToChannel(inputArrayForGreen);
        int[][] blueResult = applyDWTToChannel(inputArrayForBlue);

//        BufferedImage dwtImage = coefficientsToImage(redResult, greenResult, blueResult);
//        displayImage(dwtImage, "DWT Image");
        decodeDWT(redResult,greenResult,blueResult,n);
    }
    public int[][] createDeepCopy(int[][] input){
        int[][] result = new int[width][height];
        for (int i = 0; i < height; i++) {
            System.arraycopy(input[i], 0, result[i], 0, width);
        }
        return result;
    }
    public int[] copyRow(int[][] inputData,int rowIndex,int h){
        int copy[] = new int[h];
        for(int i=0; i < h; i++){
            copy[i] = inputData[rowIndex][i];
        }
        return copy;
    }
    public int[] copyCol(int[][] inputData,int colIndex,int w){
        int copy[] = new int[w];
        for(int i=0; i<w; i++){
            copy[i] = inputData[i][colIndex];
        }
        return copy;
    }
    public int[][] applyDWTToChannel(int[][] inputData) {

        int modifiedWidth = width;
        int modifiedHeight = height;

        int[][] transformedChannel = createDeepCopy(inputData);

        while (modifiedWidth > 1 && modifiedHeight > 1 ) {

            for (int rowIndex = 0; rowIndex < modifiedHeight; rowIndex++) {
                int currentRow[] = copyRow(transformedChannel,rowIndex,modifiedHeight);

                transformRow(currentRow);

                for(int i=0; i < modifiedHeight; i++){
                    transformedChannel[rowIndex][i] = currentRow[i];
                }
            }

            for (int colIndex = 0; colIndex < modifiedWidth; colIndex++) {
                int[] currentColumn = copyCol(transformedChannel,colIndex,modifiedWidth);

                transformRow(currentColumn);

                for (int i = 0; i < modifiedWidth; i++) {
                    transformedChannel[i][colIndex] = currentColumn[i];
                }
            }

            modifiedWidth /= 2;
            modifiedHeight /= 2;
        }


        return transformedChannel;
    }
    private void transformRow(int[] data) {
        int len = data.length;

            int[] temp = new int[len];
            int halfLen = len / 2;

            for (int i = 0; i < halfLen; i++) {

                int avg = (data[2 * i] + data[2 * i + 1]);
                int diff = (data[2 * i] - data[2 * i + 1]);

                temp[i] = avg;
                temp[halfLen + i] = diff;
            }

            for (int i = 0; i < len; i++) {
                data[i] = temp[i];
            }
    }
    private BufferedImage coefficientsToImage(int[][] red, int[][] green, int[][] blue) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int r = red[y][x] & 0xFF;
                int g = green[y][x] & 0xFF;
                int b = blue[y][x] & 0xFF;

                int pix = (r << 16) | (g << 8) | b;
                img.setRGB(x, y, pix);
            }
        }
        return img;
    }
    private void displayImage(BufferedImage img, String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(img.getWidth(), img.getHeight());
        frame.add(new JLabel(new ImageIcon(img)));
        frame.pack();
        frame.setVisible(true);
    }
    public int[][] applyIDWTToChannel(int[][] channel) {

        int[][] result = createDeepCopy(channel);

        for(int k = 0; k <= 9; k++) {
            int lowerbound = (int)Math.pow(2, k);
            if(k==0) {
                continue;
            }
            for (int rowIndex = 0; rowIndex < lowerbound; rowIndex++) {
                int[] currentRow = copyRow(result, rowIndex, lowerbound);

                inverseTransformRow(currentRow);

                for (int i = 0; i < lowerbound; i++) {
                    result[rowIndex][i] = currentRow[i];
                }
            }

            for (int colIndex = 0; colIndex < lowerbound; colIndex++) {
                int[] currentColumn = copyCol(result, colIndex, lowerbound);

                inverseTransformRow(currentColumn);

                for (int i = 0; i < lowerbound; i++) {
                    result[i][colIndex] = currentColumn[i];
                }
            }
        }
        return result;
    }
    private void inverseTransformRow(int[] data) {
        int len = data.length;
        int halfLen = len / 2;
        int[] temp = new int[len];

        for (int i = 0; i < halfLen; i++) {
            int avg = data[i];
            int diff = data[i + halfLen];
            temp[2 * i] = (avg + diff)/2;
            temp[2 * i + 1] = (avg - diff)/2;
        }

        for (int i = 0; i < len; i++) {
            data[i] = temp[i];
        }
    }
    private BufferedImage decodeDWTWithLevel(int[][] redChannel, int[][] greenChannel, int[][] blueChannel) {

        int[][] redResult = applyIDWTToChannel(redChannel);
        int[][] greenResult = applyIDWTToChannel(greenChannel);
        int[][] blueResult = applyIDWTToChannel(blueChannel);

        return coefficientsToImage(redResult, greenResult, blueResult);

    }

    private void zeroHighPassCoefficients(int[][] channel, int lowPassSize) {
        int width = channel[0].length;
        int height = channel.length;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if(i < lowPassSize && j < lowPassSize) {
                    continue;
                }
                channel[i][j] = 0;
            }
        }
    }
    public BufferedImage decodeDWT(int[][] redChannel, int[][] greenChannel, int[][] blueChannel, int n) {

        BufferedImage decodedImg = null;
        int level = 9;
        frame = new JFrame();
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 1;

        if (n == -1) {


            for (int i = 0; i <= level; i++) {
                frame.getContentPane().removeAll();

                int lowPassSize = (int) Math.pow(2, i);

                int[][] redDWTCopy = createDeepCopy(redChannel);
                int[][] greenDWTCopy = createDeepCopy(greenChannel);
                int[][] blueDWTCopy = createDeepCopy(blueChannel);

                zeroHighPassCoefficients(redDWTCopy, lowPassSize);
                zeroHighPassCoefficients(greenDWTCopy, lowPassSize);
                zeroHighPassCoefficients(blueDWTCopy, lowPassSize);

                decodedImg = decodeDWTWithLevel(redDWTCopy, greenDWTCopy, blueDWTCopy);

                lbIm1 = new JLabel(new ImageIcon(decodedImg));
                frame.getContentPane().add(lbIm1, c);
                frame.pack();
                frame.setVisible(true);

                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            int lowPassSize = (int) Math.pow(2, n);

            zeroHighPassCoefficients(redChannel, lowPassSize);
            zeroHighPassCoefficients(greenChannel, lowPassSize);
            zeroHighPassCoefficients(blueChannel, lowPassSize);

            decodedImg = decodeDWTWithLevel(redChannel, greenChannel, blueChannel);

            lbIm1 = new JLabel(new ImageIcon(decodedImg));
            frame.getContentPane().add(lbIm1, c);
            frame.pack();
            frame.setVisible(true);
        }

        return decodedImg;
    }

    public static void main(String[] args) {
        NewImageDisplay ren = new NewImageDisplay();
        ren.showIms(args);
    }

}