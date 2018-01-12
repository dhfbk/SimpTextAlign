package Preprocessing;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
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


public class ConvertAlignmentsForNN {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertAlignmentsForNN.class);
    private static final Pattern pattern = Pattern.compile("^([0-9]+):\\s+(.*)\\s+---\\(([0-9.]+)\\)--->\\s+([0-9]+):\\s+(.*)$");

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./convert-alignments")
                    .withHeader(
                            "Convert alignments to tab separated format")
                    .withOption("i", "input", "Input folder", "FILE", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withOption("t", "threshold", "Threshold", "NUM", CommandLine.Type.POSITIVE_FLOAT, true, false, false)
                    .withOption("r", "ratio", "Max ratio between text lengths", "NUM", CommandLine.Type.POSITIVE_FLOAT, true, false, false)
                    .withOption("m", "max-length", "Max length of sentence", "NUM", CommandLine.Type.POSITIVE_INTEGER, true, false, false)
                    .withOption("s", "switch", "Reverse order of sentences in final file")
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFolder = cmd.getOptionValue("input", File.class);
            File outputFile = cmd.getOptionValue("output", File.class);
            Float threshold = cmd.getOptionValue("threshold", Float.class);
            Float maxRatio = cmd.getOptionValue("ratio", Float.class);
            Integer maxLength = cmd.getOptionValue("max-length", Integer.class);
            boolean s = cmd.hasOption("switch");

            // todo: add contraints on length or on length difference

            Set<String> done = new HashSet<>();

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            for (File file : inputFolder.listFiles()) {
                for (String line : Files.readLines(file, Charsets.UTF_8)) {
                    line = line.trim();
                    if (line.length() == 0) {
                        continue;
                    }
                    Matcher matcher = pattern.matcher(line);
                    if (!matcher.find()) {
                        LOGGER.error("Line does not match! " + line);
                        continue;
                    }

                    String txt1 = matcher.group(2);
                    String txt2 = matcher.group(5);
                    if (s) {
                        txt1 = matcher.group(5);
                        txt2 = matcher.group(2);
                    }
                    Double similarity = Double.parseDouble(matcher.group(3));

                    txt1 = txt1.replace('\t', ' ');
                    txt2 = txt2.replace('\t', ' ');

                    if (maxLength != null) {
                        if (txt1.length() > maxLength || txt2.length() > maxLength) {
                            continue;
                        }
                    }

                    if (maxRatio != null) {
                        double min = Math.min(txt1.length(), txt2.length());
                        double max = Math.max(txt1.length(), txt2.length());
                        double ratio = 1.0 - min / max;
                        if (ratio > maxRatio) {
                            continue;
                        }
                    }

                    if (threshold != null && similarity < threshold) {
                        continue;
                    }

                    String hash = txt1 + "|" + txt2;
                    if (done.contains(hash)) {
                        continue;
                    }

                    writer.append(txt1).append("\t").append(txt2).append("\n");
                    done.add(hash);
                }

            }

            writer.close();

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
