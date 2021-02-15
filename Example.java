import java.io.FileInputStream; 
import java.io.FileNotFoundException; 
import javafx.application.Application;
import javafx.beans.value.ChangeListener; 
import javafx.beans.value.ObservableValue; 
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;  
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.io.*;


public class Example extends Application {
	short cthead[][][]; //store the 3D volume data set
	short min, max; //min/max value in the 3D volume data set
	int CT_x_axis = 256;
    int CT_y_axis = 256;
	int CT_z_axis = 113;
	private WritableImage topImage;
	private double skinOpacity = 0;

	@Override
    public void start(Stage stage) throws FileNotFoundException, IOException {
		stage.setTitle("CThead Viewer");

		ReadData();

		int Top_width = CT_x_axis;
		int Top_height = CT_y_axis;
		
		int Front_width = CT_x_axis;
		int Front_height = CT_z_axis;
		
		int Side_width = CT_y_axis;
		int Side_height = CT_z_axis;

		WritableImage topImage = new WritableImage(Top_width, Top_height);
		ImageView TopView = new ImageView(topImage);

		WritableImage frontImage = new WritableImage(Front_width, Front_height);
		ImageView FrontView = new ImageView(frontImage);

		WritableImage sideImage = new WritableImage(Side_width, Side_height);
		ImageView SideView = new ImageView(sideImage);

		//sliders to step through the slices (top and front directions) (remember 113 slices in top direction 0-112)
		Slider topSlider = new Slider(0, CT_z_axis-1, 0);
		Slider frontSlider = new Slider(0, CT_y_axis-1, 0);
		Slider sideSlider = new Slider(0, CT_x_axis-1, 0);
		Slider opacitySlider = new Slider(0, 100, 0);

		topSlider.valueProperty().addListener(
			(observable, oldValue, newValue) -> {
				TopDownSlice(topImage, newValue.intValue());
			});

		frontSlider.valueProperty().addListener(
			(observable, oldValue, newValue) -> {
				FrontBackSlice(frontImage, newValue.intValue());
			});

		sideSlider.valueProperty().addListener(
			(observable, oldValue, newValue) -> {
				SideSlice(sideImage, newValue.intValue());
			});

		opacitySlider.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					skinOpacity = newValue.intValue() / 100.0;
//					renderVolumes(frontImage, sideImage, topImage);
				});

		Button volumeBtn = new Button("volrend");
		volumeBtn.setOnAction(
				(ActionEvent e) -> {
					renderVolumes(frontImage, sideImage, topImage);
				}
		);

		Button gradientBtn = new Button("gradient");
		gradientBtn.setOnAction(
				(ActionEvent e) -> {
					renderGradients(frontImage, sideImage, topImage);
				}
		);

		FlowPane root = new FlowPane();
		root.setVgap(8);
        root.setHgap(4);

		root.getChildren().addAll(TopView, topSlider, FrontView, frontSlider, SideView, sideSlider, gradientBtn, volumeBtn, opacitySlider);

        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.show();
    }

	public void renderGradients(WritableImage frontImage, WritableImage sideImage, WritableImage topImage) {
		FrontGradient(frontImage);
	}

	public void FrontGradient(WritableImage image) {
		//Get image dimensions, and declare loop variables.
		int w=(int) image.getWidth(), h=(int) image.getHeight();
		PixelWriter image_writer = image.getPixelWriter();

		double col;
		// Left right, front back, up down.
		double[] vector = new double[]{-10.0, -20.0, -10.0}; // Vector of light source.
		// Light is at top right shining north-west.
		double x = CT_x_axis;
		double y = 0.0; // Most front layer.
		double z = 0.0; // At the top.

		int step = 5; // Step backwards 10 steps to calculate gradient.

		int[][] arr = new int[h][w];
		// Find all the k position where ray hits bone.
		for (int j = 0; j < h; j++) {
			for (int i = step; i < w; i++) {
				int k = 0;
				// Iterate from front to back until it reaches a bone.
				while(cthead[j][k][i] <= 400 && k < CT_y_axis - 1) {
					k += 1;
				}
				arr[j][i] = k;
			}
		}
		for (int j = 0; j < h; j++) {
			for (int i = 0; i < w; i++) {
				int k = arr[j][i];
				// Either forward step or backward step depending on i.
				int k2 = (i < step) ? arr[j][i + step] : arr[j][i - step];

				col = 1.0 - k/255.0;
				// Vector of normal (x, y, z)
				double[] lineVector = new double[]{step, k-k2, 0.0};
				double solvedX = ((k-k2)*1.0) / step;
				double[] normVector = new double[]{solvedX, 1.0, 10.0};
				double cosTheta = 0.0;
				for (int index = 0; index < 3; index++) {
					cosTheta += vector[index] * normVector[index];
				}
				cosTheta /= Math.sqrt(Math.pow(normVector[0],2) + Math.pow(normVector[1],2) + Math.pow(normVector[2],2));
				cosTheta /= Math.sqrt(Math.pow(vector[0],2) + Math.pow(vector[1],2) + Math.pow(vector[2],2));
				if (cosTheta < 0) {
					cosTheta = 0;
				}
				image_writer.setColor(i, j, Color.color(col * cosTheta, col * cosTheta, col * cosTheta, 1.0));
			}
		}
	}

    public void renderVolumes(WritableImage frontImage, WritableImage sideImage, WritableImage topImage) {
		FrontVolume(frontImage);
		SideVolume(sideImage);
		TopVolume(topImage);
	}
	
	//Function to read in the cthead data set
	public void ReadData() throws IOException {
		//File name is hardcoded here - much nicer to have a dialog to select it and capture the size from the user
		File file = new File("CThead");
		//Read the data quickly via a buffer (in C++ you can just do a single fread - I couldn't find if there is an equivalent in Java)
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		
		int i, j, k; //loop through the 3D data set
		
		min=Short.MAX_VALUE; max=Short.MIN_VALUE; //set to extreme values
		short read; //value read in
		int b1, b2; //data is wrong Endian (Order of the sequence of bytes) (check wikipedia) for Java so we need to swap the bytes around
		
		cthead = new short[CT_z_axis][CT_y_axis][CT_x_axis]; //allocate the memory - note this is fixed for this data set
		//loop through the data reading it in
		for (k=0; k<CT_z_axis; k++) {
			for (j=0; j<CT_y_axis; j++) {
				for (i=0; i<CT_x_axis; i++) {
					//because the Endianess is wrong, it needs to be read byte at a time and swapped
					b1=((int)in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types
					b2=((int)in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types
					read=(short)((b2<<8) | b1); //and swizzle the bytes around
					if (read<min) min=read; //update the minimum
					if (read>max) max=read; //update the maximum
					cthead[k][j][i]=read; //put the short into memory (in C++ you can replace all this code with one fread)
				}
			}
		}
		System.out.println(min+" "+max);
	}

	public void TopDownSlice(WritableImage image, int zIndex) {
		//Get image dimensions, and declare loop variables
		int w=(int) image.getWidth(), h=(int) image.getHeight();
		PixelWriter image_writer = image.getPixelWriter();

		double col;
		short datum;
		for (int j=0; j<h; j++) {
				for (int i=0; i<w; i++) {
						datum=cthead[zIndex][j][i]; //get values from slice 76 (change this in your assignment)
						col=(((float)datum-(float)min)/((float)(max-min)));
						image_writer.setColor(i, j, Color.color(col,col,col, 1.0));
				}
		}
    }

	public void FrontBackSlice(WritableImage image, int yIndex) {
		//Get image dimensions, and declare loop variables
		int w=(int) image.getWidth(), h=(int) image.getHeight();
		PixelWriter image_writer = image.getPixelWriter();

		double col;
		short datum;
		for (int j=0; j<h; j++) {
			for (int i=0; i<w; i++) {
				datum = cthead[j][yIndex][i];
				col=(((float)datum-(float)min)/((float)(max-min)));
				image_writer.setColor(i, j, Color.color(col,col,col, 1.0));
			}
		}
	}

	public void SideSlice(WritableImage image, int xIndex) {
		//Get image dimensions, and declare loop variables
		int w=(int) image.getWidth(), h=(int) image.getHeight();
		PixelWriter image_writer = image.getPixelWriter();

		double col;
		short datum;
		for (int j=0; j<h; j++) {
			for (int i=0; i<w; i++) {
				datum=cthead[j][i][xIndex];
				col=(((float)datum-(float)min)/((float)(max-min)));
				image_writer.setColor(i, j, Color.color(col, col, col, 1.0));
			}
		}
	}

	public Double[] transferFunction(double ctVal) {
		Double[] ans;
		if (ctVal < -300.0) {
			ans = new Double[]{0.0, 0.0, 0.0, 0.0};
			return ans;
		} else if (ctVal >= -300.0 && ctVal <= 49.0) {
			ans = new Double[]{1.0, 0.79, 0.6, 0.12};
			return ans;
		} else if (ctVal >= 50.0 && ctVal <= 299.0) {
			ans = new Double[]{0.0, 0.0, 0.0, skinOpacity};
			return ans;
		} else {
			ans = new Double[]{1.0, 1.0, 1.0, 0.8};
			return ans;
		}
	}

	public Double[][][] initializeColours(int h, int w) {
		Double[][][] colours = new Double[h][w][4];
		// Initialize array with [0, 0, 0, 1].
		for (int k=0; k<CT_y_axis; k++){
			for (int j=0; j<h; j++) {
				for (int i=0; i<w; i++) {
					for (int a = 0; a < 3; a ++) {
						colours[j][i][a] = 0.0;
					}
					colours[j][i][3] = 1.0;
				}
			}
		}
		return colours;
	}

	public void setImageWriter(PixelWriter image_writer, int h, int w, Double[][][] colours) {
		for (int j=0; j<h; j++) {
			for (int i=0; i<w; i++) {
				image_writer.setColor(i, j, Color.color(
						Math.min(1.0, colours[j][i][0]),
						Math.min(1.0, colours[j][i][1]),
						Math.min(1.0, colours[j][i][2]),
						1.0
				));
			}
		}
	}

	// Volume rendering for top down.
	public void SideVolume(WritableImage image) {
		//Get image dimensions, and declare loop variables
		int w=(int) image.getWidth(), h=(int) image.getHeight();
		PixelWriter image_writer = image.getPixelWriter();

		short datum;
		Double[][][] colours = initializeColours(h, w);

		for (int k=0; k<CT_x_axis; k++){
			for (int j=0; j<h; j++) {
				for (int i=0; i<w; i++) {
					datum = cthead[j][i][k];
					Double[] rgbValues = transferFunction(datum);
					// [r, g, b, opacity]
					if (rgbValues[3] == 1) {
						break;
					}
					for (int a = 0; a < 3; a ++) {
						// color * accumulated transparency * opacity
						colours[j][i][a] += (rgbValues[a] * colours[j][i][3] * rgbValues[3]);
					}
					// Set accumulated transparency.
					colours[j][i][3] *= (1 - rgbValues[3]);
				}
			}
		}
		setImageWriter(image_writer, h, w, colours);
	}

	// Volume rendering for top down.
	public void TopVolume(WritableImage image) {
		//Get image dimensions, and declare loop variables
		int w=(int) image.getWidth(), h=(int) image.getHeight();
		PixelWriter image_writer = image.getPixelWriter();

		short datum;
		Double[][][] colours = initializeColours(h, w);

		for (int k=0; k<CT_z_axis; k++){
			for (int j=0; j<h; j++) {
				for (int i=0; i<w; i++) {
					datum = cthead[k][j][i];
					Double[] rgbValues = transferFunction(datum);
					// [r, g, b, opacity]
					if (rgbValues[3] == 1) {
						break;
					}
					for (int a = 0; a < 3; a ++) {
						// color * accumulated transparency * opacity
						colours[j][i][a] += (rgbValues[a] * colours[j][i][3] * rgbValues[3]);
					}
					// Set accumulated transparency.
					colours[j][i][3] *= (1 - rgbValues[3]);
				}
			}
		}
		setImageWriter(image_writer, h, w, colours);
	}

	// Volume rendering for front back.
	public void FrontVolume(WritableImage image) {
		//Get image dimensions, and declare loop variables
		int w=(int) image.getWidth(), h=(int) image.getHeight();
		PixelWriter image_writer = image.getPixelWriter();

		short datum;
		Double[][][] colours = initializeColours(h, w);

		for (int k=0; k<CT_y_axis; k++){
			for (int j=0; j<h; j++) {
				for (int i=0; i<w; i++) {
					datum = cthead[j][k][i];
					Double[] rgbValues = transferFunction(datum);
					// [r, g, b, opacity]
					if (rgbValues[3] == 1) {
						break;
					}
					for (int a = 0; a < 3; a ++) {
						// color * accumulated transparency * opacity
						colours[j][i][a] += (rgbValues[a] * colours[j][i][3] * rgbValues[3]);
					}
					// Set accumulated transparency.
					colours[j][i][3] *= (1 - rgbValues[3]);
				}
			}
		}
		setImageWriter(image_writer, h, w, colours);
	}

    public static void main(String[] args) {
        launch();
    }

}