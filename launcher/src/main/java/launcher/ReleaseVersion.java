package launcher;

import java.math.BigInteger;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record ReleaseVersion(String value, BigInteger major, BigInteger minor, BigInteger patch,
        BigInteger candidate) implements Comparable<ReleaseVersion> {
    private static final String NUMBER = "(0|[1-9][0-9]*)";
    private static final Pattern PATTERN = Pattern.compile("v?" + NUMBER + "\\." + NUMBER
            + "\\." + NUMBER + "(?:-rc([1-9][0-9]*))?");

    static Optional<ReleaseVersion> parse(String value) {
        if (value == null || value.length() > 100) return Optional.empty();
        Matcher match = PATTERN.matcher(value);
        if (!match.matches()) return Optional.empty();
        return Optional.of(new ReleaseVersion(value.replaceFirst("^v", ""),
                new BigInteger(match.group(1)), new BigInteger(match.group(2)),
                new BigInteger(match.group(3)),
                match.group(4) == null ? null : new BigInteger(match.group(4))));
    }

    @Override
    public int compareTo(ReleaseVersion other) {
        int result = major.compareTo(other.major);
        if (result == 0) result = minor.compareTo(other.minor);
        if (result == 0) result = patch.compareTo(other.patch);
        if (result != 0) return result;
        if (candidate == null) return other.candidate == null ? 0 : 1;
        return other.candidate == null ? -1 : candidate.compareTo(other.candidate);
    }
}
