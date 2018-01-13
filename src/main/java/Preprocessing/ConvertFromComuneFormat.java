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

public class ConvertFromComuneFormat {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertFromComuneFormat.class);
    private static final Pattern frasePattern = Pattern.compile("<frase[^>]*>(.*)<[^>]*frase>");

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./convert-from-comune")
                    .withHeader(
                            "Convert files from Comune di Trento format")
                    .withOption("i", "input", "Input file (with list of files)", "FILE", CommandLine.Type.FILE_EXISTING, true, false, true)
                    .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFileList = cmd.getOptionValue("input", File.class);
            File outputFile = cmd.getOptionValue("output", File.class);

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            List<String> fileList = Files.readLines(inputFileList, Charsets.UTF_8);
            for (String fileName : fileList) {
                fileName = fileName.trim();
                if (fileName.length() == 0) {
                    continue;
                }
                File file = new File(fileName);
                if (!file.exists()) {
                    continue;
                }

                List<String> lines = Files.readLines(file, Charsets.UTF_8);
                for (int i = 0; i < lines.size(); i += 3) {
                    String line1 = lines.get(i).trim();
                    String line2 = lines.get(i + 1).trim();

                    Matcher matcher1 = frasePattern.matcher(line1);
                    if (matcher1.find()) {
                        line1 = matcher1.group(1);
                    }
                    Matcher matcher2 = frasePattern.matcher(line2);
                    if (matcher2.find()) {
                        line2 = matcher2.group(1);
                    }

                    line1 = line1.replaceAll("[<>]", "");
                    line2 = line2.replaceAll("[<>]", "");
                    line1 = line1.replace('\t', ' ');
                    line2 = line2.replace('\t', ' ');

                    writer.append(line1).append("\t").append(line2).append("\n");
                }
            }

            writer.close();

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
