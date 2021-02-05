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
	int VOLUME_DATA = 4;
	private WritableImage topImage;

	@Override
    public void start(Stage stage) throws FileNotFoundException, IOException {
		stage.setTitle("CThead Viewer");

		ReadData();

		//Good practice: Define your top view, front view and side view images (get the height and width correct)
		//Here's the top view - looking down on the top of the head (each slice we are looking at is CT_x_axis x CT_y_axis)
		int Top_width = CT_x_axis;
		int Top_height = CT_y_axis;
		
		//Here's the front view - looking at the front (nose) of the head (each slice we are looking at is CT_x_axis x CT_z_axis)
		int Front_width = CT_x_axis;
		int Front_height = CT_z_axis;
		
		int Side_width = CT_y_axis;
		int Side_height = CT_z_axis;

		WritableImage topImage = new WritableImage(Top_width, Top_height);
		ImageView TopView = new ImageView(topImage);

		WritableImage frontImage = new WritableImage(Front_width, Front_height);
		ImageView FrontView = new ImageView(frontImage);

		WritableImage sideImage = new WritableImage(Front_width, Front_height);
		ImageView SideView = new ImageView(sideImage);

		//sliders to step through the slices (top and front directions) (remember 113 slices in top direction 0-112)
		Slider topSlider = new Slider(0, CT_z_axis-1, 0);
		Slider frontSlider = new Slider(0, CT_y_axis-1, 0);
		Slider sideSlider = new Slider(0, CT_x_axis-1, 0);

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

		Button volumeBtn = new Button("Volume");
		volumeBtn.setOnAction(
				(ActionEvent e) -> FrontBackVolume(frontImage)
		);

		FlowPane root = new FlowPane();
		root.setVgap(8);
        root.setHgap(4);
		//https://examples.javacodegeeks.com/desktop-java/javafx/scene/image-scene/javafx-image-example/

		//3. (referring to the 3 things we need to display an image)
		//we need to add it to the flow pane
		root.getChildren().addAll(TopView, topSlider, FrontView, frontSlider, SideView, sideSlider, volumeBtn);

        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.show();
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
		System.out.println(min+" "+max); //diagnostic - for CThead this should be -1117, 2248
		//(i.e. there are 3366 levels of grey (we are trying to display on 256 levels of grey)
		//therefore histogram equalization would be a good thing
		//maybe put your histogram equalization code here to set up the mapping array
	}

	// Volume rendering for front back.
	public void FrontBackVolume(WritableImage image) {
		//Get image dimensions, and declare loop variables
		int w=(int) image.getWidth(), h=(int) image.getHeight();
		PixelWriter image_writer = image.getPixelWriter();

		short datum;
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

	
	 /*
        This function shows how to carry out an operation on an image.
        It obtains the dimensions of the image, and then loops through
        the image carrying out the copying of a slice of data into the
		image.
    */
	public void TopDownSlice(WritableImage image, int zIndex) {
		//Get image dimensions, and declare loop variables
		int w=(int) image.getWidth(), h=(int) image.getHeight();
		PixelWriter image_writer = image.getPixelWriter();

		double col;
		short datum;
		//Shows how to loop through each pixel and colour
		//Try to always use j for loops in y, and i for loops in x
		//as this makes the code more readable
		for (int j=0; j<h; j++) {
				for (int i=0; i<w; i++) {
						//at this point (i,j) is a single pixel in the image
						//here you would need to do something to (i,j) if the image size
						//does not match the slice size (e.g. during an image resizing operation
						//If you don't do this, your j,i could be outside the array bounds
						//In the framework, the image is 256x256 and the data set slices are 256x256
						//so I don't do anything - this also leaves you something to do for the assignment
						datum=cthead[zIndex][j][i]; //get values from slice 76 (change this in your assignment)
						//calculate the colour by performing a mapping from [min,max] -> 0 to 1 (float)
						//Java setColor uses float values from 0 to 1 rather than 0-255 bytes for colour
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
//				Double[] ans = transferFunction(datum);
//				image_writer.setColor(i, j, Color.color(ans[0], ans[1], ans[2], ans[3]));
			}
		}
	}

	public Double[] transferFunction(double ctVal) {
		Double[] ans;
//		System.out.println("CT VALUE: " + ctVal);
		if (ctVal < -300.0) {
			ans = new Double[]{0.0, 0.0, 0.0, 0.0};
//			System.out.println(Arrays.toString(ans));
			return ans;
		} else if (ctVal >= -300.0 && ctVal <= 49.0) {
			ans = new Double[]{1.0, 0.79, 0.6, 0.12};
//			System.out.println(Arrays.toString(ans));
			return ans;
		} else if (ctVal >= 50.0 && ctVal <= 299.0) {
			ans = new Double[]{0.0, 0.0, 0.0, 0.0};
//			System.out.println(Arrays.toString(ans));
			return ans;
		} else {
			ans = new Double[]{1.0, 1.0, 1.0, 0.8};
//			System.out.println(Arrays.toString(ans));
			return ans;
		}
	}

    public static void main(String[] args) {
        launch();
    }

}