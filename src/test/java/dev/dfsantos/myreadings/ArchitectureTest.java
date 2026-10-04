package dev.dfsantos.myreadings;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

// Traduz em verificação automatizada as convenções de arquitetura já
// documentadas em .claude/rules/code-conventions.md (ver
// docs/plans/archunit-regras-convencoes.md para a formulação exata de
// cada regra e a justificativa da API do ArchUnit 1.5.0 usada).
@AnalyzeClasses(
        packages = "dev.dfsantos.myreadings",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    private static final String ROOT = "dev.dfsantos.myreadings";
    private static final String USER_PACKAGE = ROOT + ".user";
    private static final String BOOK_PACKAGE = ROOT + ".book";
    private static final String COMMON_PACKAGE = ROOT + ".common";
    private static final String SECURITY_PACKAGE = ROOT + ".security";

    @ArchTest
    static final ArchRule controllersMustNotAccessRepositoriesDirectly = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
            .because("Controller não deve acessar Repository diretamente — "
                    + "regra de negócio pertence ao Service (code-conventions.md, seção Camadas)");

    @ArchTest
    static final ArchRule controllersMustNotDependOnCurrentUser = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().haveFullyQualifiedName(SECURITY_PACKAGE + ".CurrentUser")
            .because("CurrentUser.id() só deve ser chamado pela camada de Service "
                    + "(code-conventions.md, convenção obrigatória desde a US-04)");

    @ArchTest
    static final ArchRule onlyKnownGenericExceptionsMayLiveInCommon = classes()
            .that().haveSimpleNameEndingWith("Exception")
            .and().resideInAPackage(COMMON_PACKAGE)
            .should().haveSimpleName("NotFoundException")
            .because("common só deve conter exceções genéricas de framework/aplicação, "
                    + "nunca uma exceção de regra de negócio de user/book "
                    + "(code-conventions.md, seção 'Exception handler por módulo')");

    @ArchTest
    static final ArchRule exceptionHandlersMustBeScopedToTheirOwnModule = classes()
            .that().haveSimpleNameEndingWith("ExceptionHandler")
            .should(beAnnotatedWithRestControllerAdviceScopedToOwnPackageWhenBusinessModule())
            .because("UserExceptionHandler/BookExceptionHandler devem usar basePackages do "
                    + "próprio módulo; GlobalExceptionHandler/SecurityExceptionHandler devem "
                    + "ser agnósticos de módulo, sem basePackages "
                    + "(code-conventions.md, seção 'Exception handler por módulo')");

    @ArchTest
    static final ArchRule productionClassesMustResideInTheDefinedPackageStructure = classes()
            .should().resideInAnyPackage(
                    ROOT,
                    ROOT + ".config",
                    SECURITY_PACKAGE,
                    USER_PACKAGE,
                    USER_PACKAGE + ".dto",
                    BOOK_PACKAGE,
                    BOOK_PACKAGE + ".dto",
                    COMMON_PACKAGE
            )
            .because("a árvore de pacotes é fechada: config, security, user(.dto), book(.dto), "
                    + "common, mais MyreadingsApplication na raiz — nenhum pacote 'auth' "
                    + "remanescente (code-conventions.md, seção 'Estrutura de pacotes')");

    @ArchTest
    static final ArchRule userModuleMustNotDependOnBookModule = noClasses()
            .that().resideInAPackage(USER_PACKAGE + "..")
            .should().dependOnClassesThat().resideInAPackage(BOOK_PACKAGE + "..")
            .because("user e book são módulos de negócio irmãos, sem contrato explícito "
                    + "entre eles hoje (code-conventions.md, seção 'Estrutura de pacotes')");

    @ArchTest
    static final ArchRule bookModuleMustNotDependOnUserModule = noClasses()
            .that().resideInAPackage(BOOK_PACKAGE + "..")
            .should().dependOnClassesThat().resideInAPackage(USER_PACKAGE + "..")
            .because("user e book são módulos de negócio irmãos, sem contrato explícito "
                    + "entre eles hoje (code-conventions.md, seção 'Estrutura de pacotes')");

    @ArchTest
    static final ArchRule commonAndSecurityMustNotDependOnBusinessModules = noClasses()
            .that().resideInAnyPackage(COMMON_PACKAGE + "..", SECURITY_PACKAGE + "..")
            .should().dependOnClassesThat().resideInAnyPackage(USER_PACKAGE + "..", BOOK_PACKAGE + "..")
            .because("common e security são infraestrutura compartilhada; a direção de "
                    + "dependência é sempre user/book -> security/common, nunca o inverso "
                    + "(plano de modularização, princípio 2)");

    @ArchTest
    static final ArchRule repositoriesMustBeJpaRepositoryInterfaces = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .should().beInterfaces()
            .andShould().beAssignableTo(JpaRepository.class)
            .because("Repository é sempre uma interface Spring Data, sem lógica própria "
                    + "(code-conventions.md, seção Camadas)");

    @ArchTest
    static final ArchRule dtosMustBeNamedRequestOrResponse = classes()
            .that().resideInAnyPackage(USER_PACKAGE + ".dto", BOOK_PACKAGE + ".dto")
            .should().haveSimpleNameEndingWith("Request")
            .orShould().haveSimpleNameEndingWith("Response")
            .because("DTO de request termina em Request, DTO de resposta termina em Response "
                    + "(code-conventions.md, seção Nomenclatura)");

    private static ArchCondition<JavaClass> beAnnotatedWithRestControllerAdviceScopedToOwnPackageWhenBusinessModule() {
        return new ArchCondition<JavaClass>(
                "estar anotada com @RestControllerAdvice com basePackages == próprio pacote, "
                        + "se for de user/book, ou sem basePackages, caso contrário") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                RestControllerAdvice annotation = javaClass.tryGetAnnotationOfType(RestControllerAdvice.class)
                        .orElse(null);
                if (annotation == null) {
                    events.add(SimpleConditionEvent.violated(javaClass,
                            javaClass.getFullName() + " não está anotada com @RestControllerAdvice"));
                    return;
                }

                boolean isBusinessModuleHandler = javaClass.getPackageName().equals(USER_PACKAGE)
                        || javaClass.getPackageName().equals(BOOK_PACKAGE);
                String[] basePackages = annotation.basePackages();

                boolean satisfied = isBusinessModuleHandler
                        ? basePackages.length == 1 && basePackages[0].equals(javaClass.getPackageName())
                        : basePackages.length == 0;

                String message = satisfied
                        ? javaClass.getFullName() + " está corretamente escopada"
                        : javaClass.getFullName() + " tem basePackages " + Arrays.toString(basePackages)
                                + ", esperado " + (isBusinessModuleHandler
                                        ? "[" + javaClass.getPackageName() + "]"
                                        : "nenhum (handler global)");
                events.add(new SimpleConditionEvent(javaClass, satisfied, message));
            }
        };
    }
}
