<h1 align="center">Orrish Image Compare With Report</h1>
<h3 align="center">Uses the image compare library to compare local files and generate report.</h3>

This is an example of how to consume the image-compare library provided by OrrishLabs.

### Usage
1. Keep all png files under actual-images folder
2. Keep baselines (if exists) under baseline-images folder
3. Keep diff files under diff-images folder
4. Ensure the data in visual-comparison-data.csv is populated properly.
5. Create a jar with command ```mvn clean compile assembly:single```
6. Run the jar with below usage example.

###### Usage example
```
Pass arguments in the following order, pass default if you don't want to change.
Actual image path (optional) - Defaults to ./actual-images folder
Baseline image path (optional) - Defaults to ./baseline-images folder
Diff image path (optional) - Defaults to ./diff-images folder
Actual image pattern (optional) - Sometimes you want to compare files starting with specific texts. Pass this start pattern to identify those. Defaults to all images.
Should delete baseline for pass (optional) - If you want to delete the baseline files to save space when comparison passes. Defaults to false.
Example: java -jar image-compare-<version>.jar screenshots baseline-images diff-images default false
You can also create a visual-comparison-data.csv to ignore areas in the image with the format like below.
FILE_NAME,IGNORE_AREA,COMPARE
ALL_FILES,"0x0-237x28",true
FirstPage.png,"0x0-237x28,0x457-237x512",true
```
