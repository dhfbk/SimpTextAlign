package Preprocessing;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dh.tint.runner.TintPipeline;
import eu.fbk.utils.core.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TerenceSentenceSplitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerenceSentenceSplitter.class);
    private static final Pattern filePattern = Pattern.compile("(.*)_level([0-9])\\.txt");

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./sentence-splitting-terence")
                    .withHeader(
                            "Split Terence sentences and save files in Newsela format")
                    .withOption("i", "input", "Input folder", "FILE", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("o", "output", "Output file", "FILE", CommandLine.Type.DIRECTORY, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFolder = cmd.getOptionValue("input", File.class);
            File outputFolder = cmd.getOptionValue("output", File.class);

            TintPipeline pipeline = new TintPipeline();
            pipeline.setProperty("annotators", "ita_toksent");

            if (!outputFolder.exists()) {
                if (!outputFolder.mkdirs()) {
                    LOGGER.error("Unable to create folder");
                    System.exit(1);
                }
            }

            for (File firstLevelFolder : inputFolder.listFiles()) {
                if (!firstLevelFolder.isDirectory()) {
                    continue;
                }
                if (!firstLevelFolder.getName().toUpperCase().startsWith("IT")) {
                    continue;
                }

                for (File secondLevelFolder : firstLevelFolder.listFiles()) {
                    if (!secondLevelFolder.isDirectory()) {
                        continue;
                    }
                    Set<File> files = new HashSet<>();
                    for (File file : secondLevelFolder.listFiles()) {
                        if (!file.getName().toLowerCase().endsWith(".txt")) {
                            continue;
                        }

                        if (file.getName().toLowerCase().contains("level")) {
                            files.add(file);
                        }
                    }
                    if (files.size() == 0) {
                        continue;
                    }

                    for (File file : files) {
                        String name = file.getName();
                        Matcher matcher = filePattern.matcher(name);
                        if (!matcher.find()) {
                            LOGGER.error("Unable to match file");
                            continue;
                        }

                        String folderName = matcher.group(1);
                        int level = Integer.parseInt(matcher.group(2));

                        File saveFile = new File(outputFolder.getAbsolutePath() + File.separator + folderName + ".it." + Integer.toString(level - 1) + ".txt");
                        String text = Files.toString(file, Charsets.UTF_8);

                        BufferedWriter writer = new BufferedWriter(new FileWriter(saveFile));

                        Annotation annotation = pipeline.runRaw(text);
                        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                            writer.append(sentence.get(CoreAnnotations.TextAnnotation.class).trim()).append("\n").append("\n");
                        }

                        writer.close();

                    }

                }
            }


        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
