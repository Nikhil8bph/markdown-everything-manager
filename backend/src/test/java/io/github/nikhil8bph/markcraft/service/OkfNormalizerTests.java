package io.github.nikhil8bph.markcraft.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.nikhil8bph.markcraft.exception.VaultApiException;
import org.junit.jupiter.api.Test;

class OkfNormalizerTests {

    private final OkfNormalizer normalizer = new OkfNormalizer();

    @Test
    void preservesValidFrontmatterAndUnknownYamlExactly() {
        String content = "---\ntype: guide\ncustom: [alpha, beta]\n" 
                + "description: >\n  Long text\n---\n\n# Body\n";
        assertThat(normalizer.normalize(content, "guide.md")).isEqualTo(content);
    }

    @Test
    void insertsMissingTypeWithoutChangingOtherFieldsOrBody() {
        String content = "---\ntitle: Existing\ncustom: 42\n---\n# Body\n";
        assertThat(normalizer.normalize(content, "idea.md"))
                .isEqualTo("---\ntype: concept\ntitle: Existing\ncustom: 42\n---\n# Body\n");
        String crlf = "---\r\ntitle: Existing\r\n---\r\nBody\r\n";
        assertThat(normalizer.normalize(crlf, "idea.md"))
                .isEqualTo("---\r\ntype: concept\r\ntitle: Existing\r\n---\r\nBody\r\n");
        assertThat(normalizer.normalize("---\n{title: Existing, custom: 42}\n---\nBody", "idea.md"))
                .isEqualTo("---\n{type: concept, title: Existing, custom: 42}\n---\nBody");
        assertThat(normalizer.normalize("---\n{}\n---\n", "idea.md"))
                .isEqualTo("---\n{type: concept}\n---\n");
    }

    @Test
    void replacesEmptyAndNonStringTypeWithoutDuplicateKeys() {
        assertThat(normalizer.normalize("---\ntype: \ntitle: Keep\n---\nBody", "x.md"))
                .isEqualTo("---\ntype: concept\ntitle: Keep\n---\nBody");
        assertThat(normalizer.normalize("---\ntype: [one, two]\ncustom: yes\n---\nBody", "x.md"))
                .isEqualTo("---\ntype: concept\ncustom: yes\n---\nBody");
        assertThat(normalizer.normalize("---\ntype:\n  - one\n  - two\ncustom: yes\n---\nBody", "x.md"))
                .isEqualTo("---\ntype: concept\ncustom: yes\n---\nBody");
        assertThat(normalizer.normalize("---\ntype: \"  \"\n---\n", "x.md"))
                .isEqualTo("---\ntype: concept\n---\n");
        assertThat(normalizer.normalize("---\ntype: [] # keep comment\ntitle: Keep\n---\n", "x.md"))
                .isEqualTo("---\ntype: concept # keep comment\ntitle: Keep\n---\n");
        assertThat(normalizer.normalize("---\n{type: 42, title: Keep}\n---\n", "x.md"))
                .isEqualTo("---\n{type: concept, title: Keep}\n---\n");
    }

    @Test
    void replacesTypeAfterUnicodeWithoutDamagingOtherFields() {
        String content = "---\ntitle: 📄 Notes\ntype: 42\ncustom: keep\n---\nBody";
        assertThat(normalizer.normalize(content, "x.md"))
                .isEqualTo("---\ntitle: 📄 Notes\ntype: concept\ncustom: keep\n---\nBody");
    }

    @Test
    void prependsMinimalMetadataWithHeadingOrFilenameTitle() {
        assertThat(normalizer.normalize("# A \"quoted\" title\nBody\n", "other.md"))
                .isEqualTo("---\ntype: concept\ntitle: \"A \\\"quoted\\\" title\"\n---\n\n# A \"quoted\" title\nBody\n");
        assertThat(normalizer.normalize("Body", "my-note.md"))
                .isEqualTo("---\ntype: concept\ntitle: \"my-note\"\n---\n\nBody");
    }

    @Test
    void rejectsMalformedOrNonMappingFrontmatter() {
        for (String content : new String[] {"---\ntype: guide\nNo close", "---",
                "---\n- one\n- two\n---\n", "---\ntype: [bad\n---\n",
                "---\ntype: guide\ntype: note\n---\n"}) {
            assertThatThrownBy(() -> normalizer.normalize(content, "x.md"))
                    .isInstanceOf(VaultApiException.class)
                    .extracting("code").isEqualTo("INVALID_FRONTMATTER");
        }
    }
}
