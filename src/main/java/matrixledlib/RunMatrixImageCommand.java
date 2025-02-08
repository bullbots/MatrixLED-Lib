package matrixledlib;

import edu.wpi.first.wpilibj2.command.Command;
import matrixledlib.MatrixLEDs;
import org.opencv.core.Mat;


public class RunMatrixImageCommand extends Command {
    private final MatrixLEDs matrixLEDs;
    private final Mat matrixImage;

    public RunMatrixImageCommand(MatrixLEDs matrixLEDs, Mat matrixImage) {
        this.matrixLEDs = matrixLEDs;
        this.matrixImage = matrixImage;
        addRequirements(matrixLEDs);
    }

    @Override
    public void initialize() {
        if (matrixImage == null) {
            cancel();
            System.out.println("WARNING: RunMatrixImageCommand no matrix image.");
            return;
        }
        matrixLEDs.setMat(matrixImage);
        matrixLEDs.start();
    }

    @Override
    public boolean isFinished() {
        return false ;
    }

    @Override
    public void end(boolean interrupted) {
        super.end(interrupted);
        matrixLEDs.stop();
    }

    @Override
    public boolean runsWhenDisabled() {
        return true;
    }
}
