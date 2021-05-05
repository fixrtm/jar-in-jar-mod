package com.anatawa12.jarInJar.creator.commandline;

import com.anatawa12.jarInJar.creator.CompileConstants;
import com.anatawa12.jarInJar.creator.TargetPreset;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
Options
    -g --gui: open gui force (defaults if no parameters were passed and not a headless environment)
    -c --cui: do not open gui (defaults if some parameters were passed or a headless environment)
    -k --keep-fml-json-cache: keeps fml json caches. They're very big in general so removed by default.
    -t --target [fml-in-forge|fml-in-cpw|<version name>]: the target format or version name
    -b --base-package <package name>: the base package name
    -i --input <path to file>: sets path to input file. '-' for standard input.
    -o --output <path to file>: sets path to output file. '-' for standard output.
    -v --verbose: log all things.
 */
public class Options {
    // parsing state
    private final String[] args;
    private int argI;
    private boolean noOption = false;

    // parsed values
    LaunchMode mode = null;
    boolean keepFmlJsonCache = false;
    TargetPreset target = null;
    String basePackage = null;
    String inputFile = null;
    String outputFile = null;
    List<String> arguments = new ArrayList<>();

    boolean verbose = false;

    public Options(String[] args) {
        this.args = args;
    }

    public void parsePrams() {
        try {
            for (; argI < args.length; argI++) {
                String arg = args[argI];
                if (noOption || arg.length() < 2 || arg.charAt(0) != '-') {
                    arguments.add(arg);
                } else {
                    if (arg.equals("--")) noOption = true;
                    else if (arg.startsWith("--")) parseLongOption(arg);
                    else parseShortOption(arg);
                }
            }
        } catch (ParserException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private RuntimeException printVersion() {
        System.err.println("jar-in-jar-creator " + CompileConstants.version);
        System.err.println("(c) 2021 anatawa12 and other contributors");
        System.err.println("See README.txt and LICENSE.txt for more information.");
        System.exit(0);
        throw new RuntimeException("exit failed with 0");
    }

    private RuntimeException printHelp() {
        System.exit(0);
        throw new RuntimeException("exit failed with 0");
    }

    /*
    --gui, --cui, --target, --base-package, --input, --output
     */
    private void parseLongOption(String arg) throws ParserException {
        switch (arg) {
            case "--version":
                throw printVersion();
            case "--help":
                throw printHelp();
            case "--verbose":
                verbose = true;
                break;
            case "--gui":
                setMode(LaunchMode.GUI);
                break;
            case "--cui":
                setMode(LaunchMode.CUI);
                break;
            case "--keep-fml-json-cache":
                keepFmlJsonCache = true;
                break;
            case "--target":
                target = parseTarget(nextParam("--target"));
                break;
            case "--base-package":
                basePackage = checkPackage(nextParam("--base-package"));
                break;
            case "--input":
                inputFile = checkFile(nextParam("--input"));
                break;
            case "--output":
                outputFile = checkFile(nextParam("--output"));
                break;
            default:
                throw new ParserException("unknown option: " + arg);
        }
    }

    /*
    -g, -c, -t, -b, -i, -o
     */
    private void parseShortOption(String arg) throws ParserException {
        loop:
        for (int i = 1; i < arg.length(); i++) {
            switch (arg.charAt(i)) {
                case 'V':
                    throw printVersion();
                case 'h':
                    throw printHelp();
                case 'v':
                    verbose = true;
                    break;
                case 'g':
                    setMode(LaunchMode.GUI);
                    break;
                case 'c':
                    setMode(LaunchMode.CUI);
                    break;
                case 'k':
                    keepFmlJsonCache = true;
                    break;
                case 't':
                    target = parseTarget(nextParam('t', arg, i));
                    break loop;
                case 'b':
                    basePackage = checkPackage(nextParam('b', arg, i));
                    break loop;
                case 'i':
                    inputFile = checkFile(nextParam('i', arg, i));
                    break loop;
                case 'o':
                    outputFile = checkFile(nextParam('o', arg, i));
                    break loop;
                default:
                    throw new ParserException("unknown option: -" + arg.charAt(i));
            }
        }
    }

    private String nextParam(char optionName, String arg, int i) throws ParserException {
        if (arg.length() != i + 1) return arg.substring(i + 1);
        if (++argI == args.length) throw new ParserException("-" + optionName + " requires parameter");
        return args[argI];
    }

    private String nextParam(String optionName) throws ParserException {
        if (++argI == args.length) throw new ParserException(optionName + " requires parameter");
        return args[argI];
    }

    private void setMode(LaunchMode mode) throws ParserException {
        if (this.mode != null && this.mode != mode)
            throw new ParserException("both --gui and --cui are specified");
        this.mode = mode;
    }

    private TargetPreset parseTarget(String arg) throws ParserException {
        switch (arg) {
            case "fml-in-forge":
                return TargetPreset.FMLInForge;
            case "fml-in-cpw":
                return TargetPreset.FMLInCpw;
            default:
                Matcher matcher = versionNameRegex.matcher(arg);
                if (!matcher.matches()) throw new ParserException("invalid target: " + arg);
                int major = Integer.parseInt(matcher.group(1));
                int minor = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
                if (major < 6)
                    throw new ParserException("unsupported version: minecraft before 1.6.2 is not supported");
                if (major == 6 && minor < 2)
                    throw new ParserException("unsupported version: minecraft before 1.6.2 is not supported");
                if (major > 12)
                    throw new ParserException("unsupported version: minecraft after 1.12.2 is not supported");
                if (!(major == 7 && minor == 10) && !(major == 12 && minor == 2))
                    warning("Please make sure current target version is not tested");
                if (major < 8)
                    return TargetPreset.FMLInCpw;
                else
                    return TargetPreset.FMLInForge;
        }
    }

    private String checkPackage(String arg) throws ParserException {
        List<String> parts = new ArrayList<>();
        if (arg.length() == 0 || arg.charAt(0) == '.' || arg.charAt(0) == '/')
            throw new ParserException("invalid package name: " + arg);
        boolean hasNonCompatible = false;
        int last = 0;
        for (int i = 0; i < arg.length(); i++) {
            switch (arg.charAt(i)) {
                case '.':
                case '/':
                    if (i + 1 == arg.length() || arg.charAt(i + 1) == '.' || arg.charAt(i + 1) == '/')
                        throw new ParserException("invalid package name: " + arg);
                    String part = arg.substring(last, i);
                    parts.add(part);
                    if (!isJavaIdentifier(part))
                        hasNonCompatible = true;
                    last = i + 1;
                    break;
                case ';':
                case '[':
                    throw new ParserException("invalid package name: " + arg);
            }
        }
        if (hasNonCompatible)
            warning("package name contains non-java-compatible identifier: " + arg);
        return String.join(".", parts);
    }

    private String checkFile(String arg) throws ParserException {
        if (arg.equals("-")) return arg;
        try {
            new File(arg);
        } catch (IllegalArgumentException e) {
            throw new ParserException(e.getMessage());
        }
        return arg;
    }

    private static final Set<String> keywords = new HashSet<>(Arrays.asList(
            "abstract", "continue", "for", "new", "switch",
            "assert", "default", "goto", "package", "synchronized",
            "boolean", "do", "if", "private", "this",
            "break", "double", "implements", "protected", "throw",
            "byte", "else", "import", "public", "throws",
            "case", "enum", "instanceof", "return", "transient",
            "catch", "extends", "int", "short", "try",
            "char", "final", "interface", "static", "void",
            "class", "finally", "long", "strictfp", "volatile",
            "const", "float", "native", "super", "while"));

    private static boolean isJavaIdentifier(String identifier) {
        return !keywords.contains(identifier)
                &&Character.isJavaIdentifierStart(identifier.codePointAt(0))
                && identifier.codePoints().allMatch(Character::isJavaIdentifierPart);
    }

    List<String> warnings = new ArrayList<>();

    private void warning(String message) {
        warnings.add(message);
    }

    private static final Pattern versionNameRegex = Pattern.compile("1\\.(\\d+)(?:\\.(\\d+))?");

    public boolean anyValueParams() {
        return target != null || basePackage != null || inputFile != null || outputFile != null || !arguments.isEmpty();
    }
}
