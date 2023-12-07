import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;
//	int width = 1920; // default image width and height
//	int height = 1080;
	int width = 7680;
	int height = 4320;
	BufferedImage scaledImg;


	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
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
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
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

	public void showIms(String[] args){
//    public void showIms(String imgPath, float scale, int antiAliasing, int overlayWidth){

		// Read a parameter from command line
		String param1 = args[1];
		System.out.println("The second parameter was: " + param1);

		// Read in the specified image
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne);

		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		lbIm1 = new JLabel(new ImageIcon(imgOne));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);
	}

	public void showImsModified(String imgPath, float scale, int antiAliasing, int overlayWidth) {

		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, imgPath, imgOne);

		//ANTIALIASING
		if (antiAliasing == 1) {
			imgOne = averageFilter(imgOne, 3);
		}

		int newWidth = (int) (width * scale);
		int newHeight = (int) (height * scale);

		scaledImg = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < newHeight; y++) {
			for (int x = 0; x < newWidth; x++) {
				//Finding the corresponding coordinates in the original image
				int originalX = (int) (x / scale);
				int originalY = (int) (y / scale);
				//Setting the pixel
				scaledImg.setRGB(x, y, imgOne.getRGB(originalX, originalY));
			}
		}
//
//		//ANTIALIASING
//		if (antiAliasing == 1) {
//			scaledImg = averageFilter(scaledImg, 3);
//		}

		lbIm1 = new JLabel(new ImageIcon(scaledImg));
		lbIm1.setFocusable(true);

		//Adding key listener so that overlay window stops showing when Ctrl key is not pressed
		lbIm1.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
					lbIm1.setIcon(new ImageIcon(scaledImg));
					lbIm1.repaint();
				}
			}
		});

		lbIm1.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if (e.isControlDown()) {
					BufferedImage temp = new BufferedImage(scaledImg.getWidth(), scaledImg.getHeight(), BufferedImage.TYPE_INT_RGB);
					Graphics2D g2d = temp.createGraphics();
					//Copying the scaled Image to temp Image
					g2d.drawImage(scaledImg, 0, 0, null);

					//Getting coordinates of where mouse is
					int mouseX = e.getX();
					int mouseY = e.getY();

//					int overlayX = (int) (mouseX / scale) - overlayWidth / 2;
//					int overlayY = (int) (mouseY / scale) - overlayWidth / 2;

					int overlayX = Math.round((mouseX / scale) - overlayWidth / 2);
					int overlayY = Math.round((mouseY / scale) - overlayWidth / 2);

					for (int y = 0; y < overlayWidth; y++) {
						for (int x = 0; x < overlayWidth; x++) {
							if (overlayX + x >= 0 && overlayX + x < width && overlayY + y >= 0 && overlayY + y < height) {
								int color = imgOne.getRGB(overlayX + x, overlayY + y);
								g2d.setColor(new Color(color));
								g2d.fillRect(mouseX - overlayWidth / 2 + x, mouseY - overlayWidth / 2 + y, 1, 1);
							}
						}
					}

					g2d.dispose();
					// Setting the temp as the new image
					lbIm1.setIcon(new ImageIcon(temp));
					lbIm1.repaint();
				}
				else {
					lbIm1.setIcon(new ImageIcon(scaledImg));
					lbIm1.repaint();
				}
			}
		});

		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);
	}


    private BufferedImage averageFilter(BufferedImage img, int filterSize) {
		int offset = filterSize / 2;
		float factor = 1.0f / (filterSize * filterSize);
		BufferedImage filtered = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		for (int y = offset; y < img.getHeight() - offset; y++) {
			for (int x = offset; x < img.getWidth() - offset; x++) {
				float red = 0, green = 0, blue = 0;

				for (int ky = -offset; ky <= offset; ky++) {
					for (int kx = -offset; kx <= offset; kx++) {
						Color pixel = new Color(img.getRGB(x + kx, y + ky));
						red += pixel.getRed();
						green += pixel.getGreen();
						blue += pixel.getBlue();
					}
				}
				red *= factor;
				green *= factor;
				blue *= factor;
				Color newColor = new Color((int) red, (int) green, (int) blue);
				filtered.setRGB(x, y, newColor.getRGB());
			}
		}
		return filtered;
	}

	public static void main(String[] args) {
		//All the inputs are not provided
		if (args.length < 4) {
			System.out.println("Input Missing");
			return;
		}

		//Get all inputs.
		String imgPath = args[0];
		float scale = Float.parseFloat(args[1]);
		int antiAliasing = Integer.parseInt(args[2]);
		int overlayWidth = Integer.parseInt(args[3]);

		System.out.println("scale "+ scale);
		System.out.println("antiAliasing "+ antiAliasing);
		System.out.println("overlayWidth "+ overlayWidth);
		System.out.println("imgPath "+ imgPath);

		ImageDisplay ren = new ImageDisplay();

		ren.showImsModified(imgPath, scale, antiAliasing, overlayWidth);

	}
}
