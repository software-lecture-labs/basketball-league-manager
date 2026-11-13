package com.basketballleague.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

import com.basketballleague.domain.DomainException;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/**
 * Checks the package rules of ADR-0001 against compiled production classes.
 * The compiler cannot see these rules; a violation fails the build instead of waiting for code review.
 */
@AnalyzeClasses(packages = "com.basketballleague", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String RESOURCE = "com.basketballleague.resource..";
    private static final String SERVICE = "com.basketballleague.service..";
    private static final String REPOSITORY = "com.basketballleague.repository..";
    private static final String ENTITY = "com.basketballleague.entity..";
    private static final String DOMAIN = "com.basketballleague.domain..";
    private static final String DTO = "com.basketballleague.dto..";
    private static final String EXCEPTION = "com.basketballleague.exception..";
    private static final String CONFIG = "com.basketballleague.config..";

    private static final String RESOURCE_LAYER = "Resource";
    private static final String SERVICE_LAYER = "Service";
    private static final String REPOSITORY_LAYER = "Repository";
    private static final String ENTITY_LAYER = "Entity";
    private static final String DOMAIN_LAYER = "Domain";
    private static final String DTO_LAYER = "Dto";
    private static final String EXCEPTION_LAYER = "Exception";
    private static final String CONFIG_LAYER = "Config";

    /*
     * One-to-one copy of the "allowed project packages" table in ADR-0001. Only dependencies between these
     * packages are checked; JDK and Jakarta APIs are outside the table. Repository, entity and exception have
     * no classes yet, so they are optional; each becomes a regular layer when its first class is added.
     */
    @ArchTest
    static final ArchRule LAYER_DEPENDENCIES_FOLLOW_ADR_0001 = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer(RESOURCE_LAYER).definedBy(RESOURCE)
            .layer(SERVICE_LAYER).definedBy(SERVICE)
            .optionalLayer(REPOSITORY_LAYER).definedBy(REPOSITORY)
            .optionalLayer(ENTITY_LAYER).definedBy(ENTITY)
            .layer(DOMAIN_LAYER).definedBy(DOMAIN)
            .layer(DTO_LAYER).definedBy(DTO)
            .optionalLayer(EXCEPTION_LAYER).definedBy(EXCEPTION)
            .layer(CONFIG_LAYER).definedBy(CONFIG)
            .whereLayer(RESOURCE_LAYER).mayOnlyAccessLayers(SERVICE_LAYER, DTO_LAYER, EXCEPTION_LAYER)
            .whereLayer(SERVICE_LAYER).mayOnlyAccessLayers(REPOSITORY_LAYER, ENTITY_LAYER, DOMAIN_LAYER, DTO_LAYER)
            .whereLayer(REPOSITORY_LAYER).mayOnlyAccessLayers(ENTITY_LAYER)
            .whereLayer(ENTITY_LAYER).mayOnlyAccessLayers(DOMAIN_LAYER)
            .whereLayer(DOMAIN_LAYER).mayNotAccessAnyLayer()
            .whereLayer(DTO_LAYER).mayNotAccessAnyLayer()
            .whereLayer(EXCEPTION_LAYER).mayOnlyAccessLayers(DOMAIN_LAYER, DTO_LAYER)
            .whereLayer(CONFIG_LAYER).mayNotAccessAnyLayer()
            .because("ADR-0001 defines which project packages each package may depend on");

    // Without this rule a class outside the table (e.g. in the root package) would escape the layer check.
    @ArchTest
    static final ArchRule EVERY_CLASS_BELONGS_TO_A_DOCUMENTED_PACKAGE = classes()
            .should().resideInAnyPackage(RESOURCE, SERVICE, REPOSITORY, ENTITY, DOMAIN, DTO, EXCEPTION, CONFIG)
            .because("a new top-level package needs a decision in ADR-0001 first");

    @ArchTest
    static final ArchRule DOMAIN_IS_FRAMEWORK_FREE = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage("jakarta..", "javax..", "java.sql..")
            .because("pure business rules must be testable without a server or database (ADR-0001)");

    @ArchTest
    static final ArchRule TRANSACTIONAL_ONLY_IN_SERVICE = CompositeArchRule
            .of(noClasses().that().resideOutsideOfPackage(SERVICE)
                    .should().beAnnotatedWith(Transactional.class))
            .and(noMethods().that().areDeclaredInClassesThat().resideOutsideOfPackage(SERVICE)
                    .should().beAnnotatedWith(Transactional.class))
            .because("the use case in the service layer is the transaction boundary (ADR-0001)");

    @ArchTest
    static final ArchRule NO_FIELD_INJECTION = noFields()
            .should().beAnnotatedWith(Inject.class)
            .because("dependencies are passed through the constructor (ADR-0001)");

    // The entity package has no classes yet; remove allowEmptyShould when the first entity is added.
    @ArchTest
    static final ArchRule ENTITIES_HAVE_NO_PUBLIC_SETTERS = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage(ENTITY)
            .and().arePublic()
            .should().haveNameMatching("set[A-Z].*")
            .because("entity state changes through intention-revealing methods such as confirm() (ADR-0001)")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule DOMAIN_EXCEPTIONS_SHARE_ONE_BASE = classes()
            .that().resideInAnyPackage(DOMAIN, ENTITY)
            .and().areAssignableTo(Throwable.class)
            .should().beAssignableTo(DomainException.class)
            .because("business rule violations are translated to HTTP from a single base type");

    @ArchTest
    static final ArchRule NO_HIDDEN_CLOCK = noClasses()
            .should().callMethod(Instant.class, "now")
            .orShould().callMethod(LocalDate.class, "now")
            .orShould().callMethod(LocalDateTime.class, "now")
            .orShould().callMethod(LocalTime.class, "now")
            .orShould().callMethod(ZonedDateTime.class, "now")
            .orShould().callMethod(OffsetDateTime.class, "now")
            .orShould().callMethod(System.class, "currentTimeMillis")
            .because("time comes from an injected Clock so tests can fix it (ADR-0001)");

    @ArchTest
    static final ArchRule NO_STANDARD_STREAMS = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule NO_DUMPING_GROUND_PACKAGES = noClasses()
            .should().resideInAnyPackage("..util..", "..utils..", "..common..", "..helper..", "..helpers..", "..misc..")
            .because("every class needs a package that names its responsibility");
}
