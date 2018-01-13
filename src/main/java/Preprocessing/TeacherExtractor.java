package Preprocessing;

import com.google.common.base.Charsets;
import com.google.common.collect.TreeMultimap;
import com.google.common.io.Files;
import eu.fbk.utils.core.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TeacherExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeacherExtractor.class);
    private static final Pattern docPattern = Pattern.compile("<doc.*([os])\\.txt.*>");
    private static final Pattern frasePattern = Pattern.compile("<frase([^>]*)>(.*)<[^>]*frase>");
    private static final Pattern idPattern = Pattern.compile("id\\s*=\\s*([^a-z>]*)");
    private static final Pattern fraseAllPattern = Pattern.compile("frase_all\\s*=\\s*([^a-z>]*)");

    public static void main(String[] args) {
        try {
            final CommandLine cmd = CommandLine
                    .parser()
                    .withName("./teacher-extractor")
                    .withHeader(
                            "Extracts sentences from Teacher dataset")
                    .withOption("i", "input", "Input folder", "FILE", CommandLine.Type.DIRECTORY_EXISTING, true, false, true)
                    .withOption("o", "output", "Output file", "FILE", CommandLine.Type.FILE, true, false, true)
                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File inputFolder = cmd.getOptionValue("input", File.class);
            File outputFile = cmd.getOptionValue("output", File.class);

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            for (File file : inputFolder.listFiles()) {
                if (!file.isFile()) {
                    continue;
                }
                if (!file.getName().endsWith(".txt")) {
                    continue;
                }

                Map<Integer, String> originalSentences = new HashMap<>();
                Map<Integer, String> simplifiedSentences = new HashMap<>();
                TreeMultimap<Integer, Integer> originalMappings = TreeMultimap.create();
                TreeMultimap<Integer, Integer> simplifiedMappings = TreeMultimap.create();

                boolean original = true;
                List<String> lines = Files.readLines(file.getAbsoluteFile(), Charsets.UTF_8);
                for (String line : lines) {
                    line = line.trim();
                    if (line.length() == 0) {
                        continue;
                    }

                    Matcher docMatcher = docPattern.matcher(line);
                    if (docMatcher.find()) {
                        if (docMatcher.group(1).equals("s")) {
                            original = false;
                        }
                        continue;
                    }

                    Matcher fraseMatcher = frasePattern.matcher(line);
                    if (fraseMatcher.find()) {
                        String attr = fraseMatcher.group(1);
                        String sentence = fraseMatcher.group(2).replace('\t', ' ').replace('\n', ' ').trim();
                        Integer sentId = null;

                        Matcher idMatcher = idPattern.matcher(attr);
                        if (!idMatcher.find()) {
                            LOGGER.warn("ID not present -> " + sentence);
                            continue;
                        }

                        try {
                            sentId = Integer.parseInt(idMatcher.group(1).replaceAll("[^0-9]", ""));
                        } catch (Exception e) {
                            LOGGER.warn("Unable to retrieve sentence ID -> " + sentence);
                            continue;
                        }

                        if (original) {
                            originalSentences.put(sentId, sentence);
                        } else {
                            simplifiedSentences.put(sentId, sentence);
                        }

                        Matcher fraseAllMatcher = fraseAllPattern.matcher(attr);
                        if (!fraseAllMatcher.find()) {
                            LOGGER.warn("Links are not present -> " + sentence);
                            continue;
                        }

                        String frase_all = fraseAllMatcher.group(1).replaceAll("[^0-9;]", "").trim();
                        if (frase_all.length() == 0) {
                            LOGGER.warn("Links are empty -> " + sentence);
                            continue;
                        }

                        String[] parts = frase_all.split(";");
                        for (String part : parts) {
                            part = part.trim();
                            if (part.length() == 0) {
                                continue;
                            }
                            int partId = Integer.parseInt(part);
                            if (original) {
                                originalMappings.put(sentId, partId);
                            } else {
                                simplifiedMappings.put(sentId, partId);
                            }
                        }

                    }
                }

                Set<Integer> keysToDelete = new HashSet<>();
                for (Integer key : simplifiedMappings.keySet()) {
                    Collection<Integer> originalIDs = simplifiedMappings.get(key);
                    if (originalIDs.size() > 1) {
                        for (Integer originalID : originalIDs) {
                            originalMappings.removeAll(originalID);
                        }
                    } else {
                        keysToDelete.add(key);
                    }
                }
                for (Integer key : keysToDelete) {
                    simplifiedMappings.removeAll(key);
                }

                for (Integer key : simplifiedMappings.keySet()) {
                    Collection<Integer> originalIDs = simplifiedMappings.get(key);
                    String s2 = simplifiedSentences.get(key);
                    if (s2 == null) {
                        LOGGER.warn("s2 is null ({})", key);
                        continue;
                    }
                    StringBuffer buffer = new StringBuffer();
                    for (Integer originalID : originalIDs) {
                        String s1 = originalSentences.get(originalID);
                        if (s1 == null) {
                            LOGGER.warn("s1 is null ({})", originalID);
                            continue;
                        }
                        buffer.append(s1).append(" ");
                    }

                    String finals1 = buffer.toString().trim();

                    if (finals1.length() > 0 && s2.length() > 0) {
                        writer.append(finals1).append("\t").append(s2).append("\n");
                    }
                }

                for (Integer key : originalMappings.keySet()) {
                    Collection<Integer> simplifiedIDs = originalMappings.get(key);
                    String s1 = originalSentences.get(key);
                    if (s1 == null) {
                        LOGGER.warn("s1 is null ({})", key);
                        continue;
                    }
                    StringBuffer buffer = new StringBuffer();
                    for (Integer simplifiedID : simplifiedIDs) {
                        String s2 = simplifiedSentences.get(simplifiedID);
                        if (s2 == null) {
                            LOGGER.warn("s2 is null ({})", simplifiedID);
                            continue;
                        }
                        buffer.append(s2).append(" ");
                    }

                    String finals2 = buffer.toString().trim();

                    if (finals2.length() > 0 && s1.length() > 0) {
                        writer.append(s1).append("\t").append(finals2).append("\n");
                    }
                }
//                System.out.println(file.getName());
//                System.out.println(originalMappings);
//                System.out.println(simplifiedMappings);
//                System.out.println(originalSentences);
//                System.out.println(simplifiedSentences);
//                System.out.println();
            }

            writer.close();

        } catch (Exception e) {
            CommandLine.fail(e);
        }
    }
}
