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
import java.util.Arrays;


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

		WritableImage frontExpandedImage = new WritableImage((Front_width-1)*2 + 1, Front_height);
		ImageView FrontExpandedView = new ImageView(frontExpandedImage);

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
					renderVolumes(frontImage, sideImage, topImage);
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
					renderGradients(frontExpandedImage, sideImage, topImage);
				}
		);

		FlowPane root = new FlowPane();
		root.setVgap(8);
        root.setHgap(4);

		root.getChildren().addAll(TopView, topSlider, FrontView, frontSlider, SideView, sideSlider, opacitySlider, FrontExpandedView, gradientBtn, volumeBtn);

        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.show();
    }

	public void renderGradients(WritableImage frontImage, WritableImage sideImage, WritableImage topImage) {
		FrontRotate(frontImage);
	}

	public void FrontGradient(WritableImage image) {
		//Get image dimensions, and declare loop variables.
		int w=(int) image.getWidth(), h=(int) image.getHeight();
		PixelWriter image_writer = image.getPixelWriter();

		// Left right, front back, up down.
		double[] lightPos = new double[]{255, 0, 100}; // Coordinates of light.
		// Light is at top right shining north-west.
		double x = CT_x_axis;
		double y = 0.0; // Most front layer.
		double z = 0.0; // At the top.

		int step = 2;

		int[][] arr = new int[h][w];
		// Find all the k position where ray hits bone.
		for (int j = 0; j < h; j++) {
			for (int i = 0; i < w; i++) {
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
				int k;
				int k2;
				// Forward difference
				if (i < 1) {
					k = arr[j][i];
					k2 = arr[j][i+2];
				} else if (i == w-1) {
					// Backward difference
					k = arr[j][i-2];
					k2 = arr[j][i];
				} else {
					// Central difference
					k = arr[j][i-1];
					k2 = arr[j][i+1];
				}

				double[] lineVector = new double[]{step, k2-k, 0.0};
				double[] lightVector = new double[]{lightPos[0] - i + 1, lightPos[1] - j + 1, lightPos[2] - arr[j][i]};
				double lineLength = Math.sqrt(Math.pow(lineVector[0],2) + Math.pow(lineVector[1],2) + Math.pow(lineVector[2],2));
				double lightLength = Math.sqrt(Math.pow(lightVector[0],2) + Math.pow(lightVector[1],2) + Math.pow(lightVector[2],2));

				double cosTheta = 0.0;
				// Dot product of light and normal vector.
				for (int index = 0; index < 3; index++) {
					cosTheta += lightVector[index] * lineVector[index] / (lightLength * lineLength);
				}
				// Light is at the back, thus pixel can't be seen.
				if (cosTheta < 0) {
					cosTheta = 0;
				}

				double color = (1.0 - k/255.0) * cosTheta;
				image_writer.setColor(i, j, Color.color(color, color, color, 1.0));
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
		for (int j=0; j<h; j++) {
			for (int i=0; i<w; i++) {
				for (int a = 0; a < 3; a ++) {
					colours[j][i][a] = 0.0;
				}
				colours[j][i][3] = 1.0;
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
					// Set rgb values in array.
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

	public short[][][] rotateData() {
		// Make a larger size as 45 degrees takes up the most space.
		int largeX = (CT_x_axis-1)*2+1;
		int largeY = (CT_y_axis-1)*2+1;
		int largeZ = (CT_z_axis-1)*2+1;
		short[][][] newData = new short[largeZ][largeY][largeX];

		int xShift = 0;
		int yShift = 0;
		double angle = 360;
		double radians = Math.toRadians(angle);
		if (angle >= 0 && angle <= 90 && angle < 270 && angle <= 360) {
			xShift = CT_x_axis - 1;
			yShift = 0;
		} else if (angle > 90 && angle <= 180) {
			xShift = 360;
			yShift = CT_y_axis - 1;
		} else if (angle > 180 && angle <= 270) {
			xShift = CT_x_axis - 1;
			yShift = 360;
		}

		// Initialise all values of newData with -999.
		for (int k=0; k<CT_z_axis; k++) {
			for (int j=0; j<largeY; j++) {
				for (int i=0; i<largeX; i++) {
					newData[k][j][i] = -999;
				}
			}
		}

		double[][] transform = new double[3][3];
		// Build transform matrix.
		for (int j=0; j<3; j++) {
			for (int i=0; i<3; i++) {
				transform[j][i] = 0;
				if (j == 0 && i == 0) {
					transform[j][i] = Math.cos(radians);
				} else if (j == 0 && i == 1) {
					transform[j][i] = -1 * Math.sin(radians);
				} else if (j == 1 && i == 0) {
					transform[j][i] = Math.sin(radians);
				} else if (j == 1 && i == 1) {
					transform[j][i] = Math.cos(radians);
				} else if (j == 2 && i == 2) {
					transform[j][i] = 1;
				}
			}
		}
		// Shift each voxel to its transformed index.
		for (int k=0; k<CT_z_axis; k++){
			for (int j=0; j<CT_y_axis; j++) {
				for (int i=0; i<CT_x_axis; i++) {
					// Result of dot product.
					int zIndex = 0;
					int yIndex = 0;
					int xIndex = 0;
					int[] coordinates = new int[]{i, j, k};
					// Product of transform matrix and coordinates matrix.
					for (int a=0; a<3; a++) {
						if (a == 0) {
							for (int b = 0; b < 3; b++) {
								xIndex += coordinates[b] * transform[a][b];
							}
						} else if (a == 1) {
							for (int b = 0; b < 3; b++) {
								yIndex += coordinates[b] * transform[a][b];
							}
						} else {
							for (int b = 0; b < 3; b++) {
								zIndex += coordinates[b] * transform[a][b];
							}
						}
					}
//					System.out.println(i + " " + j + " " + k);
//					System.out.println("New: " + xIndex + " " + yIndex + " " + zIndex);
					// Map transformed index to data at original index.
					newData[zIndex][yIndex + yShift][xIndex + xShift] = cthead[k][j][i];
				}
			}
		}
		return newData;
	}

	// Volume rendering for front back.
	public void FrontRotate(WritableImage image) {
		//Get image dimensions, and declare loop variables
		int w=(int) image.getWidth(), h=(int) image.getHeight();
		PixelWriter image_writer = image.getPixelWriter();

		short datum;
		Double[][][] colours = initializeColours(h, w);

		short[][][] newData = rotateData();

		for (int k=0; k<(CT_y_axis - 1)*2 + 1; k++){
			for (int j=0; j<h; j++) {
				for (int i=0; i<w; i++) {
					datum = newData[j][k][i];
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