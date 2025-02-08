// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package matrixledlib;

import edu.wpi.first.wpilibj.Filesystem;
import matrixledlib.MatrixLEDs;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** Add your docs here. */
public class YamlLoader {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private static final List<String> filenames = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, Mat> images = Collections.synchronizedMap(new HashMap<>());
//    private static final Map<String, List<Mat>> videos = Collections.synchronizedMap(new HashMap<>());
    private static final HashMap<String, List<Mat>> videos = new HashMap<>();
    private static final ReentrantLock lock = new ReentrantLock();

    public static void load() {
        Thread thread = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            try {
                lock.lock();
                try {
                    Thread.sleep(8000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                File[] files = Paths.get(Filesystem.getDeployDirectory().toString(), "matrixled").toFile().listFiles();
                ArrayList<String> f = new ArrayList<>();
                for (File file : files) {
                    if (file.getName().endsWith(".yaml")) {
                        filenames.add(file.getName());

                        System.out.printf("Info: found file: %s\n", file.getName());
                    }
                }

                loadMatImages();
                loadYamlFiles();
                System.out.printf("Info: YamlLoader loaded files in %.04f seconds.\n", (System.currentTimeMillis() - startTime) / 1000.);
            } finally {
                lock.unlock();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    // static {
    //     long startTime = System.currentTimeMillis();
    //     File[] files = Paths.get(Filesystem.getDeployDirectory().toString(), "").toFile().listFiles();
    //     ArrayList<String> f = new ArrayList<>();
    //     for (File file : files) {
    //         if (file.getName().endsWith(".yaml")) {
    //             filenames.add(file.getName());
                
    //             System.out.printf("Info: found file: %s\n",  file.getName());
    //         }
    //     }

    //     loadMatImages();
    //     loadYamlFiles();
    //     System.out.printf("Info: YamlLoader loaded files in %.04f seconds.\n", (System.currentTimeMillis() - startTime) / 1000.);
    // }

    private static void loadMatImages() {
        // We need these for debugging the Matrix LEDs physical configuration.
        images.put("Off", MatrixLEDs.off());
        images.put("Row One", MatrixLEDs.oneRow(0));
        images.put("Row Two", MatrixLEDs.oneRow(1));
        images.put("Col One", MatrixLEDs.oneCol(0));
        images.put("Col Two", MatrixLEDs.oneCol(1));
        images.put("Eye", MatrixLEDs.eye());
    }

    private static void putImage(String key, Mat value) {
        System.out.println("Info: Yaml put image: " + key);
        images.put(key, value);
    }

    public static Mat getImage(String key) {
        if (images.containsKey(key)) {
            return images.get(key);
        }

        return null;
        
        // // Make sure the sad face loaded too.
        // Mat ret = images.get("sad-face-frown");
        // if (ret == null) {
        //     return new Mat(16, 16, CvType.CV_8UC3);
        // }
        // return ret;
    }

    private static List<Mat> putVideo(String key, List<Mat> value) {
        System.out.println("Info: Yaml put video: " + key);
        return videos.put(key, value);
    }

    public static List<Mat> getVideo(String key) {
        if (lock.tryLock()) {
            try {
                return videos.getOrDefault(key, null);
            } finally {
                lock.unlock();
            }
        } else {
            return null;
        }

        // // Make sure the sad face loaded too.
        // List<Mat> ret = videos.get("sad-face-frown-one-frame");
        // if (ret == null) {
        //     return List.of(new Mat(16, 16, CvType.CV_8UC3));
        // }
        // return ret;
    }

    private static Mat getMatFromFrame(Map<String, Object> imageData, String frameName) {
        List<List<List<Integer>>> frameArray = (List<List<List<Integer>>>) imageData.get(frameName);
        int rows = frameArray.size();
        int cols = frameArray.get(0).size();
        Mat frame = new Mat(rows, cols, CvType.CV_8UC3);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                List<Integer> pixel = frameArray.get(row).get(col);
                double[] pixelData = {pixel.get(0), pixel.get(1), pixel.get(2)};
                frame.put(row, col, pixelData);
            }
        }
        return frame;
    }

    private static void loadImageYaml(Path filePath) {
        // Try handling as an image
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(filePath.toFile());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            }
            Map<String, Object> imageData = yaml.load(inputStream);

            String imageName = (String) imageData.get("image_name");

            Mat frame = getMatFromFrame(imageData, "frame");

            putImage(imageName, frame);

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }

    private static void loadVideoYaml(Path filePath) {
        try {
            Yaml yaml = new Yaml(new Constructor(ArrayList.class));
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(filePath.toFile());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            }
            List<Map<String, Object>> gifDataList = yaml.load(inputStream);
            for (Map<String, Object> gifData : gifDataList) {
                String gif_name = (String) gifData.get("gif_name");
                List<Map<String, Object>> framesData = (List<Map<String, Object>>) gifData.get("frames");
                List<Mat> gifFrames = new ArrayList<>();

                for (Map<String, Object> frameData : framesData) {

                    Mat frame = getMatFromFrame(frameData, "frame_data");

                    gifFrames.add(frame);
                }

                putVideo(gif_name, gifFrames);
            }

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }

    private static void loadYamlFiles() {
        Path deployPath = Paths.get(Filesystem.getDeployDirectory().toString(), "matrixled");

        for (String filename : filenames) {
            Path filePath = deployPath.resolve(filename);

            // Lightweight check to see if the yaml file is an image or video file.
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
                int firstChar = reader.read();
                if (firstChar != 1) {
                    if ('{' == (char) firstChar) {
                        loadImageYaml(filePath);
                    } else {
                        loadVideoYaml(filePath);
                    }
                } else {
                    System.out.printf("WARNING: The YAML file is empty: %s%n", filename);
                }
            } catch (IOException e) {
                System.out.printf("ERROR: reading YAML file: %s%n", e.getMessage());
            }
        }
    }
}
