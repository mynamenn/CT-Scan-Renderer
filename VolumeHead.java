import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class VolumeHead extends Main {
    short cthead[][][];

    public VolumeHead(short cthead[][][]) {
        this.cthead = cthead;
    }

    // Volume rendering for top down.
    public void SideVolume(WritableImage image, double skinOpacity) {
        //Get image dimensions, and declare loop variables
        int w=(int) image.getWidth(), h=(int) image.getHeight();
        PixelWriter image_writer = image.getPixelWriter();

        short datum;
        Double[][][] colours = initializeColours(h, w);

        for (int k=0; k<CT_x_axis; k++){
            for (int j=0; j<h; j++) {
                for (int i=0; i<w; i++) {
                    datum = cthead[j][i][k];
                    Double[] rgbValues = transferFunction(datum, skinOpacity);
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
    public void TopVolume(WritableImage image, double skinOpacity) {
        //Get image dimensions, and declare loop variables
        int w=(int) image.getWidth(), h=(int) image.getHeight();
        PixelWriter image_writer = image.getPixelWriter();

        short datum;
        Double[][][] colours = initializeColours(h, w);

        for (int k=0; k<CT_z_axis; k++){
            for (int j=0; j<h; j++) {
                for (int i=0; i<w; i++) {
                    datum = cthead[k][j][i];
                    Double[] rgbValues = transferFunction(datum, skinOpacity);
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

    // Volume rendering for front back.
    public void FrontVolume(WritableImage image, double skinOpacity) {
        //Get image dimensions, and declare loop variables
        int w=(int) image.getWidth(), h=(int) image.getHeight();
        PixelWriter image_writer = image.getPixelWriter();

        short datum;
        Double[][][] colours = initializeColours(h, w);

        for (int k=0; k<CT_y_axis; k++){
            for (int j=0; j<h; j++) {
                for (int i=0; i<w; i++) {
                    datum = cthead[j][k][i];
                    Double[] rgbValues = transferFunction(datum, skinOpacity);
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
}
