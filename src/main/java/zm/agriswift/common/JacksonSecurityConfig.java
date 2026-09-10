package zm.agriswift.common;

import org.springframework.boot.jackson.autoconfigure.JsonFactoryBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.SerializableString;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.io.SerializedString;

@Configuration
public class JacksonSecurityConfig {

    /**
     * XSS hardening at the serialization layer: every JSON string value emitted by
     * this API has <, >, &, ' and " HTML-escaped, so stored payloads can never
     * execute even if a client renders them unsafely.
     */
    @Bean
    public JsonFactoryBuilderCustomizer htmlEscapingFactoryCustomizer() {
        return builder -> builder.characterEscapes(new HtmlCharacterEscapes());
    }

    public static class HtmlCharacterEscapes extends CharacterEscapes {
        private final int[] asciiEscapes;

        public HtmlCharacterEscapes() {
            asciiEscapes = CharacterEscapes.standardAsciiEscapesForJSON();
            asciiEscapes['<'] = CharacterEscapes.ESCAPE_CUSTOM;
            asciiEscapes['>'] = CharacterEscapes.ESCAPE_CUSTOM;
            asciiEscapes['&'] = CharacterEscapes.ESCAPE_CUSTOM;
            asciiEscapes['\''] = CharacterEscapes.ESCAPE_CUSTOM;
            asciiEscapes['"'] = CharacterEscapes.ESCAPE_CUSTOM;
        }

        @Override
        public int[] getEscapeCodesForAscii() {
            return asciiEscapes;
        }

        @Override
        public SerializableString getEscapeSequence(int ch) {
            return new SerializedString(
                    switch (ch) {
                        case '<' -> "&lt;";
                        case '>' -> "&gt;";
                        case '&' -> "&amp;";
                        case '\'' -> "&#39;";
                        case '"' -> "&quot;";
                        default -> "";
                    }
            );
        }
    }
}