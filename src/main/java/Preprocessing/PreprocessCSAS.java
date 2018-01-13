package Preprocessing;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import eu.fbk.utils.core.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class PreprocessCSAS {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreprocessCSAS.class);
//    private static final Pattern months = Pattern.compile("[^\\w](gennaio|febbraio|marzo|aprile|maggio|giugno|luglio|agosto|settembre|ottobre|novembre|dicembre)[^\\w]");

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./preprocess-csas")
                    .withHeader(
                            "Preprocess CSAS dataset")
                    .withOption("i", "input", "Input file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFile = cmd.getOptionValue("input", File.class);
            File outputFile = cmd.getOptionValue("output", File.class);

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            List<String> lines = Files.readLines(inputFile, Charsets.UTF_8);
            boolean firstLine = true;
            for (String line : lines) {
                line = line.trim();
                String[] parts = line.split("\t");
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                if (parts.length < 2) {
                    continue;
                }

                String before = parts[0];
                String after = parts[1];

                String hNums = before.replaceAll("[^0-9]", "");
                String eNums = after.replaceAll("[^0-9]", "");

                if (!hNums.equals(eNums)) {
                    continue;
                }

                before = before.replace('\t', ' ').trim();
                after = after.replace('\t', ' ').trim();
                writer.append(before).append("\t").append(after).append("\n");
            }

            writer.close();

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
