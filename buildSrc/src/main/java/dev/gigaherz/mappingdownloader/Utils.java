package dev.gigaherz.mappingdownloader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils
{
    public static final CSVFormat CUSTOM_CSV_FORMAT = CSVFormat.DEFAULT.withFirstRecordAsHeader();

    static final Pattern LEGAL_NAME = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");
    private static final Pattern LEGAL_SRG = Pattern.compile("^(?:func|field|p)_(?:[a-zA-Z]*)([0-9]+)_(?:[a-zA-Z0-9_]*)$");
    private static final Pattern PARAM_SRG = Pattern.compile("^p_(i?)([0-9]+)_([0-9]+)_?$");

    static boolean isNullOrEmpty(String s)
    {
        return s == null || s.length() == 0;
    }

    static int compareById(CSVRecord r0, CSVRecord r1)
    {
        String srgName0 = r0.get(0);
        String srgName1 = r1.get(0);
        Matcher srgMatcher0 = LEGAL_SRG.matcher(srgName0);
        Matcher srgMatcher1 = LEGAL_SRG.matcher(srgName1);
        boolean isValid0 = srgMatcher0.matches();
        boolean isValid1 = srgMatcher1.matches();
        if (isValid0 && !isValid1)
        {
            return -1;
        }
        else if (isValid1 & !isValid0)
        {
            return 1;
        }
        else if (isValid0 && isValid1)
        {
            int id0 = Integer.parseInt(srgMatcher0.group(1));
            int id1 = Integer.parseInt(srgMatcher1.group(1));
            if (id0 != id1)
            {
                return Integer.compare(id0, id1);
            }
            return String.CASE_INSENSITIVE_ORDER.compare(srgName0, srgName1);
        }
        else
        {
            return String.CASE_INSENSITIVE_ORDER.compare(srgName0, srgName1);
        }
    }

    public static boolean isValidSrg(String s)
    {
        Matcher m = LEGAL_SRG.matcher(s);
        return m.matches();
    }

    public static Integer getId(String s)
    {
        Matcher m = LEGAL_SRG.matcher(s);
        if (!m.matches())
            throw new IllegalStateException("Invalid SRG: " + s);
        return Integer.parseInt(m.group(1));
    }

    public static Integer getIdParam(String s)
    {
        Matcher m = PARAM_SRG.matcher(s);
        if (!m.matches())
            throw new IllegalStateException("Invalid SRG: " + s);
        int sign = isNullOrEmpty(m.group(1)) ? 1 : -1;
        int id = Integer.parseInt(m.group(2));
        int arg = Integer.parseInt(m.group(3));
        return sign * ((id << 8) + arg);
    }

    public static void decodeParam(String s, TriConsumer<Boolean, Integer, Integer> logic)
    {
        Matcher m = PARAM_SRG.matcher(s);
        if (!m.matches())
            throw new IllegalStateException("Invalid SRG: " + s);
        boolean isConstructor = !isNullOrEmpty(m.group(1));
        int id = Integer.parseInt(m.group(2));
        int arg = Integer.parseInt(m.group(3));
        logic.accept(isConstructor, id, arg);
    }

    @FunctionalInterface
    public interface TriConsumer<A,B,C>
    {
        public void accept(A a, B b, C c);
    }
}
