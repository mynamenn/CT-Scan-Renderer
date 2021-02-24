import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class GradientHead extends Main {
    short cthead[][][];

    public GradientHead(short cthead[][][]) {
        this.cthead = cthead;
    }

    public void FrontGradient(WritableImage image) {
        //Get image dimensions, and declare loop variables.
        int w=(int) image.getWidth(), h=(int) image.getHeight();
        PixelWriter image_writer = image.getPixelWriter();
        int step = 2;

        // Left right, front back, up down.
        double[] lightPos = new double[]{255, 0, 100}; // Coordinates of light.

        int[][] arr = new int[h][w]; // Array of positions of k where ray hits bone.
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
                double x = (k2-k)/step;
                double[] normalVector = new double[]{x,-1 , 0.0};
                double[] lightVector = new double[]{lightPos[0] - i, lightPos[1] - j, lightPos[2] - arr[j][i]};
                double lineLength = Math.sqrt(Math.pow(normalVector[0],2) + Math.pow(normalVector[1],2) + Math.pow(normalVector[2],2));
                double lightLength = Math.sqrt(Math.pow(lightVector[0],2) + Math.pow(lightVector[1],2) + Math.pow(lightVector[2],2));

                double cosTheta = 0.0;
                // Dot product of light and normal vector.
                for (int index = 0; index < 3; index++) {
                    cosTheta += lightVector[index] * normalVector[index] / (lightLength * lineLength);
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
}
