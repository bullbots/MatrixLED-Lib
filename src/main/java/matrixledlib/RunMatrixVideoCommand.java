package matrixledlib;

import edu.wpi.first.wpilibj2.command.Command;
import matrixledlib.MatrixLEDs;
import org.opencv.core.Mat;

import java.util.List;


public class RunMatrixVideoCommand extends Command {
    private final MatrixLEDs matrixLEDs;
    private final List<Mat> matrixImages;
    private int robotCyclesPerFrame = 0;
    private int curIdx = 0;
    private int prevIdx = 0;
    private int curCycle = 0;
    private boolean isFinished = false;

    public enum RunType {
        ONCE,
        CONTINUOUS
    }

    private RunType runType;


    public RunMatrixVideoCommand(MatrixLEDs matrixLEDs, List<Mat> matrixImages, int robotCyclesPerFrame, RunType runType) {
        this.matrixLEDs = matrixLEDs;
        this.matrixImages = matrixImages;
        this.robotCyclesPerFrame = robotCyclesPerFrame;
        this.runType = runType;
        addRequirements(matrixLEDs);
    }

    @Override
    public void initialize() {
        if (matrixImages == null) {
            cancel();
            System.out.println("WARNING: RunMatrixVideoCommand no matrix images.");
            return;
        }
        curIdx = 0;
        prevIdx = 0;
        curCycle = 0;
        isFinished = false;
        matrixLEDs.setMat(matrixImages.get(0));
        matrixLEDs.start();
    }

    @Override
    public void execute() {
        curCycle++;
        if (curCycle % robotCyclesPerFrame == 0) {
            curIdx++;
            curCycle = 0;
        }

        if (curIdx >= matrixImages.size() && runType == RunType.ONCE) {
            isFinished = true;
        } else if (curIdx >= matrixImages.size()) {
            // System.out.println("INFO: restarting video");
            curIdx = 0;
        }

        if (!isFinished && curIdx != prevIdx) {
            matrixLEDs.setMat(matrixImages.get(curIdx));
            matrixLEDs.start();
        }
        prevIdx = curIdx;
    }

    @Override
    public boolean isFinished() {
        if (runType == RunType.CONTINUOUS) {
            return false;
        }

        return isFinished;
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
