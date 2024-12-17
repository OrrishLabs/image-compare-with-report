package com.orrish.automation.entrypoint;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.orrish.automation.model.CompareSpec;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompareAndReport {

    static final String reportHtml = "visual-regression.html";
    static final String csvFileName = "visual-comparison-data.csv";
    static final String ANSI_BLACK = "\u001B[30m";
    static final String ANSI_RED = "\u001B[31m";
    static final String ANSI_GREEN = "\u001B[32m";
    static final String ANSI_YELLOW = "\u001B[33m";
    static final String ANSI_RESET = "\u001B[0m";
    static final String ANSI_PURPLE = "\u001B[35m";
    static final String ANSI_CYAN = "\u001B[36m";
    static final String ANSI_BLUE = "\u001B[34m";
    static final String ANSI_WHITE = "\u001B[37m";
    static final String BOLD_TEXT = "\033[0;1m";

    public static void main(String[] args) throws Exception {

        System.out.println(
                BOLD_TEXT + ANSI_PURPLE + "Pass arguments in the following order, pass default if you don't want to change." + ANSI_RESET + System.lineSeparator() +
                        BOLD_TEXT + ANSI_GREEN + "Actual image path " + ANSI_RESET + ANSI_CYAN + "(optional)" + ANSI_RESET + " - Defaults to ./actual-images folder" + System.lineSeparator() +
                        BOLD_TEXT + ANSI_GREEN + "Baseline image path " + ANSI_RESET + ANSI_CYAN + "(optional)" + ANSI_RESET + " - Defaults to ./baseline-images folder" + System.lineSeparator() +
                        BOLD_TEXT + ANSI_GREEN + "Diff image path " + ANSI_RESET + ANSI_CYAN + "(optional)" + ANSI_RESET + " - Defaults to ./diff-images folder" + System.lineSeparator() +
                        BOLD_TEXT + ANSI_GREEN + "Actual image pattern " + ANSI_RESET + ANSI_CYAN + "(optional)" + ANSI_RESET + " - Sometimes you want to compare files starting with specific texts. Pass this start pattern to identify those. Defaults to all images." + System.lineSeparator() +
                        BOLD_TEXT + ANSI_GREEN + "Should delete baseline for pass " + ANSI_RESET + ANSI_CYAN + "(optional)" + ANSI_RESET + " - If you want to delete the baseline files to save space when comparison passes. Defaults to false." + System.lineSeparator() +
                        BOLD_TEXT + ANSI_YELLOW + "Example: " + ANSI_RESET + ANSI_YELLOW + "java -jar image-compare-<version>.jar screenshots baseline-images diff-images default false" + ANSI_RESET);
        System.out.println(BOLD_TEXT + ANSI_GREEN + "You can also create a visual-comparison-data.csv to ignore areas in the image with the format like below." + System.lineSeparator() + ANSI_RESET +
                ANSI_BLUE + "FILE_NAME,IGNORE_AREA,COMPARE" + System.lineSeparator() +
                "ALL_FILES,\"0x0-237x28\",true" + System.lineSeparator() +
                "FirstPage.png,\"0x0-237x28,0x457-237x512\",true" + ANSI_RESET);

        String actualImageFolderName = (args.length > 0) && !args[0].trim().equals("default") ? args[0] : "actual-images";
        String baselineImageFolderName = (args.length > 1) && !args[1].trim().equals("default") ? args[1] : "baseline-images";
        String diffImageFolderName = (args.length > 2) && !args[2].trim().equals("default") ? args[2] : "diff-images";
        String actualImagePattern = (args.length > 3) && !args[3].trim().equals("default") ? args[3] : "default";

        boolean deleteBaselineForPass = false;
        if (args.length > 4) {
            if (Arrays.stream((new String[]{"true", "false"})).noneMatch(e -> e.equals(args[4]))) {
                System.out.println(BOLD_TEXT + ANSI_RED + "You have to pass either true or false for delete baseline option." + ANSI_RESET);
                return;
            }
            deleteBaselineForPass |= Boolean.parseBoolean(args[4]);
        }

        Path actualFolderPath = Paths.get(actualImageFolderName);
        if (Files.notExists(actualFolderPath))
            Files.createDirectory(actualFolderPath);
        Path baselineFolderPath = Paths.get(baselineImageFolderName);
        if (Files.notExists(baselineFolderPath))
            Files.createDirectory(baselineFolderPath);
        Path diffFolderPath = Paths.get(diffImageFolderName);
        if (Files.notExists(diffFolderPath)) {
            Files.createDirectory(diffFolderPath);
        }

        ExtentReports extentReports = initializeExtentReport();

        List<CompareSpec> linesInCSV = new ArrayList<>();
        try {
            try (Reader reader = Files.newBufferedReader(Paths.get(csvFileName))) {
                CsvToBean csvToBean = new CsvToBeanBuilder(reader).withType(CompareSpec.class).build();
                linesInCSV = csvToBean.parse();
            }
        } catch (Exception ex) {
        }
        List<Path> result;
        try (Stream<Path> walk = Files.walk(actualFolderPath)) {
            result = walk.filter(Files::isRegularFile).collect(Collectors.toList());
        }
        HashMap<Status, Integer> totalResult = new HashMap<>();
        totalResult.put(Status.PASS, 0);
        totalResult.put(Status.FAIL, 0);
        for (Path eachActualImagePath : result) {
            String fileName = eachActualImagePath.getFileName().toString();
            if (!fileName.isEmpty() && !actualImagePattern.equals("default") && !fileName.startsWith(actualImagePattern))
                continue;
            Stream<CompareSpec> csvEntry = linesInCSV.stream().filter(e -> e.fileName.equals("ALL_FILES"));
            String ignoreArea = "";
            if (csvEntry.count() != 0)
                ignoreArea = linesInCSV.stream().filter(e -> e.fileName.equals("ALL_FILES")).findFirst().get().ignoreArea;
            if (linesInCSV.stream().filter(e -> e.fileName.equals(fileName)).count() != 0) {
                CompareSpec line = linesInCSV.stream().filter(e -> e.fileName.equals(fileName)).findFirst().get();
                ignoreArea += (ignoreArea.isEmpty() ? "" : ",") + line.ignoreArea;
                if (!line.compare) {
                    continue;
                }
            }

            Path baselinePath = Paths.get(baselineImageFolderName + File.separator + fileName);
            Path diffPath = Paths.get(diffImageFolderName + File.separator + fileName);
            ExtentTest extentTest = extentReports.createTest(fileName);
            if (!baselinePath.toFile().exists()) {
                extentTest.fail("No Baseline : " + extentTest.addScreenCaptureFromPath(eachActualImagePath.toString()));
                System.out.println(Status.FAIL + " :: " + fileName + " :: No baseline");
                totalResult.put(Status.FAIL, totalResult.get(Status.FAIL) + 1);
                continue;
            }
            ImageCompare imageCompare = new ImageCompare()
                    .setActual(ImageIO.read(eachActualImagePath.toFile()))
                    .setBaseline(ImageIO.read(baselinePath.toFile()));

            try {
                if (!ignoreArea.trim().isEmpty()) {
                    String[] areas = ignoreArea.split(",");
                    for (String area : areas) {
                        String startArea = area.split("-")[0].toUpperCase();
                        String endArea = area.split("-")[1].toUpperCase();
                        imageCompare.setIgnoreRegion(getX(startArea), getY(startArea), getX(endArea), getY(endArea));
                    }
                }
            } catch (Exception ex) {
                extentTest.fail("Some issues with ignore area input for : " + fileName + System.lineSeparator() + ex);
                continue;
            }
            CompareResult compareResult = imageCompare.compareImage();

            if (compareResult.isSame()) {
                totalResult.put(Status.PASS, totalResult.get(Status.PASS) + 1);
            } else {
                totalResult.put(Status.FAIL, totalResult.get(Status.FAIL) + 1);
            }
            compareResult.setDiffColor(Color.RED)
                    .setDiffFile(diffImageFolderName + File.separator + fileName)
                    .saveDiffImage();

            Status status = compareResult.isSame() ? Status.PASS : Status.FAIL;
            if (status == Status.PASS && deleteBaselineForPass) {
                baselinePath.toFile().delete();
                extentTest.log(status, "" + extentTest.addScreenCaptureFromPath(eachActualImagePath.toString()));
            } else {
                extentTest.log(status, "Image order : Baseline, Actual, Diff" + extentTest
                        .addScreenCaptureFromPath(baselinePath.toString())
                        .addScreenCaptureFromPath(eachActualImagePath.toString())
                        .addScreenCaptureFromPath(diffPath.toString()));
            }
            System.out.println(status + " :: " + fileName + " :: Total pix diff: " + compareResult.getDiffPixelCount()
                    + ", Total pix : " + compareResult.getTotalPixelCount()
                    + ", Total pix diff %age: " + compareResult.getDiffPixelPercentage()
                    + ", isSame: " + compareResult.isSame());
        }
        int pass = totalResult.get(Status.PASS);
        int fail = totalResult.get(Status.FAIL);
        int total = pass + fail;
        System.out.println("Visual Check: " + (pass == total ? "PASS" : "FAIL") + " : Pass - " + pass + ", Fail - " + fail + ", Total - " + total);
        extentReports.flush();
    }

    private static ExtentReports initializeExtentReport() {
        ExtentHtmlReporter extentSparkReporter = new ExtentHtmlReporter(reportHtml);
        extentSparkReporter.config().setDocumentTitle("Visual Regression");
        extentSparkReporter.config().setEncoding("utf-8");
        extentSparkReporter.config().setReportName("Visual Report");
        extentSparkReporter.config().setChartVisibilityOnOpen(false);
        ExtentReports extentReports = new ExtentReports();
        extentReports.attachReporter(extentSparkReporter);
        return extentReports;
    }

    private static int getY(String area) {
        return Integer.parseInt(area.split("X")[1]);
    }

    private static int getX(String area) {
        return Integer.parseInt(area.split("X")[0]);
    }

}
