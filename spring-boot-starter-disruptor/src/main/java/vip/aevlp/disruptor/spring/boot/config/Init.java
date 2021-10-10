package vip.aevlp.disruptor.spring.boot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.aevlp.disruptor.spring.boot.exception.EventHandleException;
import vip.aevlp.disruptor.spring.boot.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Init implements Map<String, Section> {

    private static transient final Logger log = LoggerFactory.getLogger(Init.class);

    public static final String DEFAULT_SECTION_NAME = ""; //empty string means the first unnamed section

    private static final String COMMENT_POUND = "#";
    private static final String COMMENT_SEMICOLON = ";";
    private static final String SECTION_PREFIX = "[";
    private static final String SECTION_SUFFIX = "]";

    private final Map<String, Section> sections;

    /**
     * Creates a new empty {@code Ini} instance.
     */
    public Init() {
        this.sections = new LinkedHashMap<>();
    }

    /**
     * Creates a new {@code Ini} instance with the specified defaults.
     *
     * @param defaults the default sections and/or key-value pairs to copy into the new instance.
     */
    public Init(Init defaults) {
        this();
        if (defaults == null) {
            throw new NullPointerException("Defaults cannot be null.");
        }
        for (Section section : defaults.getSections()) {
            Section copy = new Section(section);
            this.sections.put(section.getName(), copy);
        }
    }

    /**
     * Returns {@code true} if no sections have been configured, or if there are sections, but the sections themselves
     * are all empty, {@code false} otherwise.
     *
     * @return {@code true} if no sections have been configured, or if there are sections, but the sections themselves
     * are all empty, {@code false} otherwise.
     */
    @Override
    public boolean isEmpty() {
        Collection<Section> sections = this.sections.values();
        if (!sections.isEmpty()) {
            for (Section section : sections) {
                if (!section.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns the names of all sections managed by this {@code Ini} instance or an empty collection if there are
     * no sections.
     *
     * @return the names of all sections managed by this {@code Ini} instance or an empty collection if there are
     * no sections.
     */
    public Set<String> getSectionNames() {
        return Collections.unmodifiableSet(sections.keySet());
    }

    /**
     * Returns the sections managed by this {@code Ini} instance or an empty collection if there are
     * no sections.
     *
     * @return the sections managed by this {@code Ini} instance or an empty collection if there are
     * no sections.
     */
    private Collection<Section> getSections() {
        return Collections.unmodifiableCollection(sections.values());
    }

    /**
     * Returns the {@link Section} with the given name or {@code null} if no section with that name exists.
     *
     * @param sectionName the name of the section to retrieve.
     * @return the {@link Section} with the given name or {@code null} if no section with that name exists.
     */
    public Section getSection(String sectionName) {
        String name = cleanName(sectionName);
        return sections.get(name);
    }

    /**
     * Ensures a section with the specified name exists, adding a new one if it does not yet exist.
     *
     * @param sectionName the name of the section to ensure existence
     * @return the section created if it did not yet exist, or the existing Section that already existed.
     */
    private Section addSection(String sectionName) {
        String name = cleanName(sectionName);
        Section section = getSection(name);
        if (section == null) {
            section = new Section(name);
            this.sections.put(name, section);
        }
        return section;
    }

    /**
     * Removes the section with the specified name and returns it, or {@code null} if the section did not exist.
     *
     * @param sectionName the name of the section to remove.
     * @return the section with the specified name or {@code null} if the section did not exist.
     */
    public Section removeSection(String sectionName) {
        String name = cleanName(sectionName);
        return this.sections.remove(name);
    }

    private static String cleanName(String sectionName) {
        String name = StringUtils.trimToNull(sectionName);
        if (name == null) {
            log.trace("Specified name was null or empty.  Defaulting to the default section (name = \"\")");
            name = DEFAULT_SECTION_NAME;
        }
        return name;
    }

    /**
     * Sets a name/value pair for the section with the given {@code sectionName}.  If the section does not yet exist,
     * it will be created.  If the {@code sectionName} is null or empty, the name/value pair will be placed in the
     * default (unnamed, empty string) section.
     *
     * @param sectionName   the name of the section to add the name/value pair
     * @param propertyName  the name of the property to add
     * @param propertyValue the property value
     */
    public void setSectionProperty(String sectionName, String propertyName, String propertyValue) {
        String name = cleanName(sectionName);
        Section section = getSection(name);
        if (section == null) {
            section = addSection(name);
        }
        section.put(propertyName, propertyValue);
    }

    /**
     * Returns the value of the specified section property, or {@code null} if the section or property do not exist.
     *
     * @param sectionName  the name of the section to retrieve to acquire the property value
     * @param propertyName the name of the section property for which to return the value
     * @return the value of the specified section property, or {@code null} if the section or property do not exist.
     */
    private String getSectionProperty(String sectionName, String propertyName) {
        Section section = getSection(sectionName);
        return section != null ? section.get(propertyName) : null;
    }

    /**
     * Returns the value of the specified section property, or the {@code defaultValue} if the section or
     * property do not exist.
     *
     * @param sectionName  the name of the section to add the name/value pair
     * @param propertyName the name of the property to add
     * @param defaultValue the default value to return if the section or property do not exist.
     * @return the value of the specified section property, or the {@code defaultValue} if the section or
     * property do not exist.
     */
    public String getSectionProperty(String sectionName, String propertyName, String defaultValue) {
        String value = getSectionProperty(sectionName, propertyName);
        return value != null ? value : defaultValue;
    }


    /**
     * Loads the specified raw INI-formatted text into this instance.
     *
     * @param iniConfig the raw INI-formatted text to load into this instance.
     * @throws EventHandleException if the text cannot be loaded
     */
    public void load(String iniConfig) throws EventHandleException {
        load(new Scanner(iniConfig));
    }

    /**
     * Loads the INI-formatted text backed by the given InputStream into this instance.  This implementation will
     * close the input stream after it has finished loading.  It is expected that the stream's contents are
     * UTF-8 encoded.
     *
     * @param is the {@code InputStream} from which to read the INI-formatted text
     * @throws IOException if unable
     */
    public void load(InputStream is) throws IOException {
        if (is == null) {
            throw new NullPointerException("InputStream argument cannot be null.");
        }
        InputStreamReader isr;
        isr = new InputStreamReader(is, StandardCharsets.UTF_8);
        load(isr);
    }

    /**
     * Loads the INI-formatted text backed by the given Reader into this instance.  This implementation will close the
     * reader after it has finished loading.
     *
     * @param reader the {@code Reader} from which to read the INI-formatted text
     */
    private void load(Reader reader) {
        Scanner scanner = new Scanner(reader);
        try {
            load(scanner);
        } finally {
            try {
                scanner.close();
            } catch (Exception e) {
                log.debug("Unable to cleanly close the InputStream scanner.  Non-critical - ignoring.", e);
            }
        }
    }

    private void addSection(String name, StringBuilder content) {
        if (content.length() > 0) {
            String contentString = content.toString();
            String cleaned = StringUtils.trimToNull(contentString);
            if (cleaned != null) {
                Section section = new Section(name, contentString);
                if (!section.isEmpty()) {
                    sections.put(name, section);
                }
            }
        }
    }

    /**
     * Loads the INI-formatted text backed by the given Scanner.  This implementation will close the
     * scanner after it has finished loading.
     *
     * @param scanner the {@code Scanner} from which to read the INI-formatted text
     */
    private void load(Scanner scanner) {

        String sectionName = DEFAULT_SECTION_NAME;
        StringBuilder sectionContent = new StringBuilder();

        while (scanner.hasNextLine()) {

            String rawLine = scanner.nextLine();
            String line = StringUtils.trimToNull(rawLine);

            if (line == null || line.startsWith(COMMENT_POUND) || line.startsWith(COMMENT_SEMICOLON)) {
                //skip empty lines and comments:
                continue;
            }

            String newSectionName = getSectionName(line);
            if (newSectionName != null) {
                //found a new section - convert the currently buffered one into a Section object
                addSection(sectionName, sectionContent);

                //reset the buffer for the new section:
                sectionContent = new StringBuilder();

                sectionName = newSectionName;

                if (log.isDebugEnabled()) {
                    log.debug("Parsing " + SECTION_PREFIX + sectionName + SECTION_SUFFIX);
                }
            } else {
                //normal line - add it to the existing content buffer:
                sectionContent.append(rawLine).append("\n");
            }
        }

        //finish any remaining buffered content:
        addSection(sectionName, sectionContent);
    }

    private static boolean isSectionHeader(String line) {
        String s = StringUtils.trimToNull(line);
        return s != null && s.startsWith(SECTION_PREFIX) && s.endsWith(SECTION_SUFFIX);
    }

    private static String getSectionName(String line) {
        String s = StringUtils.trimToNull(line);
        if (isSectionHeader(s)) {
            return cleanName(s.substring(1, s.length() - 1));
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Init) {
            Init init = (Init) obj;
            return this.sections.equals(init.sections);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.sections.hashCode();
    }

    @Override
    public String toString() {
        if (this.sections == null || this.sections.isEmpty()) {
            return "<empty INI>";
        } else {
            StringBuilder sb = new StringBuilder("sections=");
            int i = 0;
            for (Section section : this.sections.values()) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(section.toString());
                i++;
            }
            return sb.toString();
        }
    }

    @Override
    public int size() {
        return this.sections.size();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.sections.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.sections.containsValue(value);
    }

    @Override
    public Section get(Object key) {
        return this.sections.get(key);
    }

    @Override
    public Section put(String key, Section value) {
        return this.sections.put(key, value);
    }

    @Override
    public Section remove(Object key) {
        return this.sections.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Section> m) {
        this.sections.putAll(m);
    }

    @Override
    public void clear() {
        this.sections.clear();
    }

    @Override
    public Set<String> keySet() {
        return Collections.unmodifiableSet(this.sections.keySet());
    }

    @Override
    public Collection<Section> values() {
        return Collections.unmodifiableCollection(this.sections.values());
    }

    @Override
    public Set<Entry<String, Section>> entrySet() {
        return Collections.unmodifiableSet(this.sections.entrySet());
    }

}