package vip.aevlp.disruptor.spring.boot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.aevlp.disruptor.spring.boot.util.StringUtils;

import java.util.*;

import static vip.aevlp.disruptor.spring.boot.config.Init.DEFAULT_SECTION_NAME;


/**
 * An {@code Ini.Section} is String-key-to-String-value Map, identifiable by a
 * {@link #getName() name} unique within an {@link Init} instance.
 */
public class Section implements Map<String, String> {

    private static transient final Logger log = LoggerFactory.getLogger(Section.class);
    private static final char ESCAPE_TOKEN = '\\';
    private final String name;
    private final Map<String, String> props;

    Section(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        this.name = name;
        this.props = new LinkedHashMap<String, String>();
    }

    Section(String name, String sectionContent) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        this.name = name;
        Map<String, String> props;
        if (StringUtils.isNotBlank(sectionContent)) {
            props = toMapProps(sectionContent);
        } else {
            props = new LinkedHashMap<>();
        }
        if (props != null) {
            this.props = props;
        } else {
            this.props = new LinkedHashMap<>();
        }
    }

    Section(Section defaults) {
        this(defaults.getName());
        putAll(defaults.props);
    }

    //Protected to access in a test case - NOT considered part of Shiro's public API

    private static boolean isContinued(String line) {
        if (StringUtils.isBlank(line)) {
            return false;
        }
        int length = line.length();
        //find the number of backslashes at the end of the line.  If an even number, the
        //backslashes are considered escaped.  If an odd number, the line is considered continued on the next line
        int backslashCount = 0;
        for (int i = length - 1; i > 0; i--) {
            if (line.charAt(i) == ESCAPE_TOKEN) {
                backslashCount++;
            } else {
                break;
            }
        }
        return backslashCount % 2 != 0;
    }

    private static boolean isKeyValueSeparatorChar(char c) {
        return Character.isWhitespace(c) || c == ':' || c == '=';
    }

    private static boolean isCharEscaped(CharSequence s, int index) {
        return index > 0 && s.charAt(index - 1) == ESCAPE_TOKEN;
    }

    //Protected to access in a test case - NOT considered part of Shiro's public API
    static String[] splitKeyValue(String keyValueLine) {
        String line = StringUtils.trimToNull(keyValueLine);
        if (line == null) {
            return null;
        }
        StringBuilder keyBuffer = new StringBuilder();
        StringBuilder valueBuffer = new StringBuilder();

        boolean buildingKey = true; //we'll build the value next:

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (buildingKey) {
                if (isKeyValueSeparatorChar(c) && !isCharEscaped(line, i)) {
                    buildingKey = false;//now start building the value
                } else {
                    keyBuffer.append(c);
                }
            } else {
                if (valueBuffer.length() == 0 && isKeyValueSeparatorChar(c) && !isCharEscaped(line, i)) {
                    //swallow the separator chars before we start building the value
                } else {
                    valueBuffer.append(c);
                }
            }
        }

        String key = StringUtils.trimToNull(keyBuffer.toString());
        String value = StringUtils.trimToNull(valueBuffer.toString());

        if (key == null || value == null) {
            String msg = "Line argument must contain a key and a value.  Only one string token was found.";
            throw new IllegalArgumentException(msg);
        }

        log.trace("Discovered key/value pair: {}={}", key, value);

        return new String[]{key, value};
    }

    @SuppressWarnings("resource")
    private static Map<String, String> toMapProps(String content) {
        Map<String, String> props = new LinkedHashMap<String, String>();
        String line;
        StringBuilder lineBuffer = new StringBuilder();
        Scanner scanner = new Scanner(content);
        while (scanner.hasNextLine()) {
            line = StringUtils.trimToNull(scanner.nextLine());
            if (isContinued(line)) {
                //strip off the last continuation backslash:
                line = line.substring(0, line.length() - 1);
                lineBuffer.append(line);
                continue;
            } else {
                lineBuffer.append(line);
            }
            line = lineBuffer.toString();
            lineBuffer = new StringBuilder();
            String[] kvPair = splitKeyValue(line);
            props.put(kvPair[0], kvPair[1]);
        }

        return props;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void clear() {
        this.props.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.props.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.props.containsValue(value);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return this.props.entrySet();
    }

    @Override
    public String get(Object key) {
        return this.props.get(key);
    }

    @Override
    public boolean isEmpty() {
        return this.props.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return this.props.keySet();
    }

    @Override
    public String put(String key, String value) {
        return this.props.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        this.props.putAll(m);
    }

    @Override
    public String remove(Object key) {
        return this.props.remove(key);
    }

    @Override
    public int size() {
        return this.props.size();
    }

    @Override
    public Collection<String> values() {
        return this.props.values();
    }

    @Override
    public String toString() {
        String name = getName();
        if (DEFAULT_SECTION_NAME.equals(name)) {
            return "<default>";
        }
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Section) {
            Section other = (Section) obj;
            return getName().equals(other.getName()) && this.props.equals(other.props);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() * 31 + this.props.hashCode();
    }
}
