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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApplyFlags {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplyFlags.class);
    private static final Pattern flagPattern = Pattern.compile("^(#[A-Z]+\\s+)");

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./remove-flags")
                    .withHeader(
                            "Remove flags and save TSV format")
                    .withOption("i", "input", "Input file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFile = cmd.getOptionValue("input", File.class);
            File outputFile = cmd.getOptionValue("output", File.class);

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            List<String> lines = Files.readLines(inputFile, Charsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                Matcher matcher = flagPattern.matcher(line);
                if (matcher.find()) {
                    String flag = matcher.group(1);
                    if (flag != null && flag.toLowerCase().trim().equals("#deleted")) {
                        continue;
                    }
                    line = line.substring(flag.length()).trim();
                }

                writer.append(line).append("\n");
            }

            writer.close();

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
