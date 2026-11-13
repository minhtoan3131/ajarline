package vn.io.ajarline.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Checks the package rules that keep capabilities independent.
 *
 * <p>The rules read compiled production classes. They cover only packages
 * that exist today. A rule for a new capability is added together with the
 * first real code of that capability.</p>
 */
class CapabilityBoundaryTest {

    private static final String ROOT = "vn.io.ajarline";
    private static final String[] CAPABILITIES = {
            ROOT + ".content..",
            ROOT + ".publication..",
            ROOT + ".administration.."
    };

    // Importing classes is slow, so all rules share one import.
    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void capabilities_ShouldHaveNoDependencyCycle_WhenProductionClassesAreImported() {
        ArchRule rule = slices()
                .matching(ROOT + ".(*)..")
                .should().beFreeOfCycles();

        rule.check(PRODUCTION_CLASSES);
    }

    @ParameterizedTest(name = "[{index}] {0} uses only {1}.contract")
    @CsvSource({
            "administration, content",
            "administration, publication",
            "publication,    content",
            "publication,    administration",
            "content,        publication",
            "content,        administration"
    })
    void access_ShouldUseOnlyContract_WhenCapabilityUsesAnotherCapability(
            String consumer, String provider) {
        String owner = ROOT + "." + provider;
        ArchRule rule = noClasses()
                .that().resideInAPackage(ROOT + "." + consumer + "..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        owner + ".api..",
                        owner + ".application..",
                        owner + ".model..",
                        owner + ".infrastructure..");

        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void platform_ShouldNotDependOnCapabilities_WhenProductionClassesAreImported() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(ROOT + ".platform..")
                .should().dependOnClassesThat().resideInAnyPackage(CAPABILITIES);

        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void entities_ShouldStayInPersistencePackage_WhenMappedWithJpa() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should().resideInAPackage("..infrastructure.persistence..");

        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void contentInfrastructure_ShouldBeUsedOnlyByContent_WhenOtherPackagesNeedArticles() {
        // Contracts and API types are not in the allowed list, so an entity
        // can never leak through them.
        ArchRule rule = classes()
                .that().resideInAPackage(ROOT + ".content.infrastructure..")
                .should().onlyBeAccessed().byAnyPackage(
                        ROOT + ".content.application..",
                        ROOT + ".content.infrastructure..");

        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void publicationAndAdministration_ShouldNotUsePersistence_WhenChangingContent() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                        ROOT + ".publication..",
                        ROOT + ".administration..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..",
                        "org.springframework.data..");

        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void transactions_ShouldStartInApplicationServices_WhenAnnotationIsUsed() {
        ArchRule onMethods = noMethods()
                .that().areDeclaredInClassesThat().resideOutsideOfPackage("..application..")
                .should().beAnnotatedWith(Transactional.class);
        ArchRule onClasses = noClasses()
                .that().resideOutsideOfPackage("..application..")
                .should().beAnnotatedWith(Transactional.class);

        onMethods.check(PRODUCTION_CLASSES);
        onClasses.check(PRODUCTION_CLASSES);
    }

    @Test
    void controllers_ShouldNotUseRepositories_WhenHandlingRequests() {
        ArchRule rule = noClasses()
                .that().areAnnotatedWith(RestController.class)
                .should().dependOnClassesThat().areAssignableTo(Repository.class);

        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void controllers_ShouldResideInApiPackage_WhenDeclared() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .should().resideInAPackage("..api..");

        rule.check(PRODUCTION_CLASSES);
    }

    @Test
    void controllers_ShouldFollowPathOwner_WhenMappingAdminOrPublicPaths() {
        ArchRule adminSurface = classes()
                .that().areAnnotatedWith(RestController.class)
                .and(mapPathUnder("/api/admin/", "/api/auth/"))
                .should().resideInAPackage(ROOT + ".administration.api");
        ArchRule publicSurface = classes()
                .that().areAnnotatedWith(RestController.class)
                .and(mapPathUnder("/api/v1/"))
                .should().resideOutsideOfPackage(ROOT + ".administration..");

        adminSurface.check(PRODUCTION_CLASSES);
        publicSurface.check(PRODUCTION_CLASSES);
    }

    /**
     * Matches a controller whose class-level mapping starts with one of the
     * prefixes. Method-level paths are not read; every controller here
     * declares its base path on the class.
     */
    private static DescribedPredicate<JavaClass> mapPathUnder(String... prefixes) {
        return DescribedPredicate.describe(
                "map a path under " + String.join(" or ", prefixes),
                javaClass -> javaClass.tryGetAnnotationOfType(RequestMapping.class)
                        .map(mapping -> Stream.concat(
                                Arrays.stream(mapping.value()),
                                Arrays.stream(mapping.path())))
                        .orElseGet(Stream::empty)
                        .map(CapabilityBoundaryTest::fromRoot)
                        .anyMatch(path -> Arrays.stream(prefixes).anyMatch(path::startsWith)));
    }

    private static String fromRoot(String path) {
        // Spring treats "api/x" and "/api/x" as the same path.
        return path.startsWith("/") ? path : "/" + path;
    }
}
