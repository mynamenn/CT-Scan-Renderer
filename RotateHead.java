import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

public class RotateHead extends Main {
    short cthead[][][];

    public RotateHead(short cthead[][][]) {
        this.cthead = cthead;
    }

    public short[][][] rotateData(double angle) {
        // Make a larger size as 45 degrees takes up the most space.
        int largeX = (CT_x_axis-1)*2+1;
        int largeY = (CT_y_axis-1)*2+1;
        int largeZ = (CT_z_axis-1)*2+1;
        short[][][] newData = new short[largeZ][largeY][largeX];
        int xShift = 0;
        int yShift = 0;
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
    public void FrontRotate(WritableImage image, double angle) {
        //Get image dimensions, and declare loop variables
        int w=(int) image.getWidth(), h=(int) image.getHeight();
        PixelWriter image_writer = image.getPixelWriter();

        short datum;
        Double[][][] colours = initializeColours(h, w);

        short[][][] newData = rotateData(angle);

        for (int k=0; k<(CT_y_axis - 1)*2 + 1; k++){
            for (int j=0; j<h; j++) {
                for (int i=0; i<w; i++) {
                    datum = newData[j][k][i];
                    Double[] rgbValues = transferFunction(datum, 0.12);
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
