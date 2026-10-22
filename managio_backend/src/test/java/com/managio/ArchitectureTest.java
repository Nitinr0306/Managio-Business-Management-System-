package com.managio;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Keeps the package structure described in docs/ARCHITECTURE.md: feature modules under
 * {@code com.managio.modules.<feature>} split into layers that only point one way, and shared platform code that
 * no feature depends back on.
 */
@AnalyzeClasses(packages = "com.managio", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String CONTROLLER = "com.managio.modules..controller..";
    private static final String SERVICE = "com.managio.modules..service..";
    private static final String REPOSITORY = "com.managio.modules..repository..";
    private static final String DOMAIN = "com.managio.modules..domain..";
    private static final String DTO = "com.managio.modules..dto..";

    /** A read-model query's own filter enums and row records, which an endpoint may take or return. */
    private static final DescribedPredicate<JavaClass> QUERY_TYPES =
            resideInAPackage(REPOSITORY).and(DescribedPredicate.describe("nested", JavaClass::isNestedClass));

    @ArchTest
    static final ArchRule layersPointOneWay = layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Controller").definedBy(CONTROLLER)
            .layer("Service").definedBy(SERVICE)
            .layer("Repository").definedBy(REPOSITORY)
            .layer("Dto").definedBy(DTO)
            .layer("Domain").definedBy(DOMAIN)
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Dto").mayOnlyBeAccessedByLayers("Controller", "Service", "Repository")
            .ignoreDependency(resideInAPackage(CONTROLLER), QUERY_TYPES);

    @ArchTest
    static final ArchRule domainStandsAlone = noClasses().that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(CONTROLLER, SERVICE, REPOSITORY, DTO);

    @ArchTest
    static final ArchRule platformDoesNotKnowFeatures = noClasses().that().resideInAPackage("com.managio.platform..")
            .should().dependOnClassesThat().resideInAPackage("com.managio.modules..");

    @ArchTest
    static final ArchRule controllersLiveInControllerPackages = classes().that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage(CONTROLLER);

    @ArchTest
    static final ArchRule entitiesLiveInDomainPackages = classes().that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage(DOMAIN);

    @ArchTest
    static final ArchRule repositoriesLiveInRepositoryPackages = classes()
            .that().areAssignableTo(org.springframework.data.repository.Repository.class).and().areInterfaces()
            .should().resideInAPackage(REPOSITORY);

    @ArchTest
    static final ArchRule servicesLiveInServicePackages = classes().that().areAnnotatedWith(Service.class)
            .should().resideInAPackage(SERVICE);
}
