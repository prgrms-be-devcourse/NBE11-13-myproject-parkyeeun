package com.repoary.backend.analysis.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisFileFilterTest {

    private final AnalysisFileFilter analysisFileFilter =
            new AnalysisFileFilter();

    @Test
    @DisplayName("TIL 생성에 필요한 코드와 문서 파일을 포함한다")
    void includeRelevantFiles() {
        assertThat(
                analysisFileFilter.isRelevant(
                        "src/main/java/com/example/UserService.java"
                )
        ).isTrue();

        assertThat(
                analysisFileFilter.isRelevant(
                        "src/main/resources/application.yml"
                )
        ).isTrue();

        assertThat(
                analysisFileFilter.isRelevant(
                        "docs/security.md"
                )
        ).isTrue();

        assertThat(
                analysisFileFilter.isRelevant(
                        "build.gradle"
                )
        ).isTrue();

        assertThat(
                analysisFileFilter.isRelevant(
                        "frontend/src/App.tsx"
                )
        ).isTrue();

        assertThat(
                analysisFileFilter.isRelevant(
                        "src/main/resources/templates/login.html"
                )
        ).isTrue();
    }

    @Test
    @DisplayName("Git 실행 파일과 Git 설정 파일을 제외한다")
    void excludeGitAndGradleExecutableFiles() {
        assertThat(
                analysisFileFilter.isRelevant("gradlew")
        ).isFalse();

        assertThat(
                analysisFileFilter.isRelevant("gradlew.bat")
        ).isFalse();

        assertThat(
                analysisFileFilter.isRelevant(".gitignore")
        ).isFalse();

        assertThat(
                analysisFileFilter.isRelevant(".gitattributes")
        ).isFalse();
    }

    @Test
    @DisplayName("lock 파일을 제외한다")
    void excludeLockFiles() {
        assertThat(
                analysisFileFilter.isRelevant("package-lock.json")
        ).isFalse();

        assertThat(
                analysisFileFilter.isRelevant("yarn.lock")
        ).isFalse();

        assertThat(
                analysisFileFilter.isRelevant("pnpm-lock.yaml")
        ).isFalse();
    }

    @Test
    @DisplayName("빌드 산출물과 의존성 디렉터리 내부 파일을 제외한다")
    void excludeGeneratedDirectories() {
        assertThat(
                analysisFileFilter.isRelevant(
                        "backend/build/classes/java/main/User.class"
                )
        ).isFalse();

        assertThat(
                analysisFileFilter.isRelevant(
                        "frontend/node_modules/react/index.js"
                )
        ).isFalse();

        assertThat(
                analysisFileFilter.isRelevant(
                        "backend/target/generated-sources/Test.java"
                )
        ).isFalse();

        assertThat(
                analysisFileFilter.isRelevant(
                        "frontend/dist/assets/index.js"
                )
        ).isFalse();

        assertThat(
                analysisFileFilter.isRelevant(
                        "backend/.gradle/cache.properties"
                )
        ).isFalse();
    }

    @Test
    @DisplayName("바이너리 파일과 지원하지 않는 확장자를 제외한다")
    void excludeUnsupportedExtensions() {
        assertThat(
                analysisFileFilter.isRelevant(
                        "gradle/wrapper/gradle-wrapper.jar"
                )
        ).isFalse();

        assertThat(
                analysisFileFilter.isRelevant(
                        "src/main/resources/static/logo.png"
                )
        ).isFalse();

        assertThat(
                analysisFileFilter.isRelevant(
                        "document.pdf"
                )
        ).isFalse();

        assertThat(
                analysisFileFilter.isRelevant(
                        "README"
                )
        ).isFalse();
    }

    @Test
    @DisplayName("확장자 대소문자와 윈도우 경로 구분자를 정규화한다")
    void normalizePathAndExtension() {
        assertThat(
                analysisFileFilter.isRelevant(
                        "src\\main\\java\\com\\example\\Main.JAVA"
                )
        ).isTrue();

        assertThat(
                analysisFileFilter.isRelevant(
                        "docs\\README.MD"
                )
        ).isTrue();

        assertThat(
                analysisFileFilter.isRelevant(
                        "frontend\\NODE_MODULES\\react\\index.JS"
                )
        ).isFalse();
    }

    @Test
    @DisplayName("null 또는 빈 경로는 제외한다")
    void excludeNullOrBlankPath() {
        assertThat(
                analysisFileFilter.isRelevant(null)
        ).isFalse();

        assertThat(
                analysisFileFilter.isRelevant("")
        ).isFalse();

        assertThat(
                analysisFileFilter.isRelevant("   ")
        ).isFalse();
    }
}