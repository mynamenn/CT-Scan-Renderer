import java.io.FileInputStream; 
import java.io.FileNotFoundException; 
import javafx.application.Application;
import javafx.beans.value.ChangeListener; 
import javafx.beans.value.ObservableValue; 
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.io.*;

public class Main extends Application {
	short cthead[][][]; //store the 3D volume data set
	short min, max; //min/max value in the 3D volume data set
	int CT_x_axis = 256;
    int CT_y_axis = 256;
	int CT_z_axis = 113;
	private double skinOpacity = 0.12;
	private double angle = 0; // Angle of rotation.
	RotateHead rotate;
	GradientHead gradient;
	VolumeHead volume;

	@Override
    public void start(Stage stage) throws IOException {
		stage.setTitle("CThead Viewer");
		ReadData();
		rotate = new RotateHead(cthead);
		gradient = new GradientHead(cthead);
		volume = new VolumeHead(cthead);

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

		Slider topSlider = new Slider(0, CT_z_axis-1, 0);
		Slider frontSlider = new Slider(0, CT_y_axis-1, 0);
		Slider sideSlider = new Slider(0, CT_x_axis-1, 0);
		Slider opacitySlider = new Slider(0, 0.5, skinOpacity);
		Slider angleSlider = new Slider(0, 360, angle);

		Label opacityLabel =  new Label("Opacity slider");

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
					skinOpacity = (double) newValue;
				});

		angleSlider.valueProperty().addListener(
				(observable, oldValue, newValue) -> {
					angle = (double) newValue;
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

		Button rotateBtn = new Button("rotate");
		rotateBtn.setOnAction(
				(ActionEvent e) -> {
					renderRotate(frontExpandedImage, sideImage, topImage);
				}
		);

		FlowPane root = new FlowPane();
		root.setVgap(8);
        root.setHgap(4);

		root.getChildren().addAll(
				TopView, topSlider,
				FrontView, frontSlider,
				SideView, sideSlider,
				FrontExpandedView, angleSlider,
				opacityLabel, opacitySlider,
				rotateBtn, gradientBtn, volumeBtn
		);

        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.show();
    }

	public void renderGradients(WritableImage frontImage, WritableImage sideImage, WritableImage topImage) {
		gradient.FrontGradient(frontImage);
	}

	public void renderVolumes(WritableImage frontImage, WritableImage sideImage, WritableImage topImage) {
		volume.FrontVolume(frontImage, skinOpacity);
		volume.SideVolume(sideImage, skinOpacity);
		volume.TopVolume(topImage, skinOpacity);
	}

	public void renderRotate(WritableImage frontImage, WritableImage sideImage, WritableImage topImage) {
		rotate.FrontRotate(frontImage, angle);
	}
	
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

	public Double[] transferFunction(double ctVal, double skinOpacity) {
		if (ctVal < -300) {
			return new Double[]{0.0, 0.0, 0.0, 0.0};
		} else if (ctVal <= 49) {
			return new Double[]{1.0, 0.79, 0.6, skinOpacity};
		} else if (ctVal <= 299) {
			return new Double[]{0.0, 0.0, 0.0, 0.0};
		} else if (ctVal <= 4096) {
			return new Double[]{1.0, 1.0, 1.0, 0.8};
		} else {
			return new Double[]{0.0, 0.0, 0.0, 0.0};
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

    public static void main(String[] args) {
        launch();
    }

}