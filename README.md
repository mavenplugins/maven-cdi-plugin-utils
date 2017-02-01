CDI-based Dependency Injection for Maven Plugin Development
===========================================================
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.itemis.maven.plugins/cdi-plugin-utils/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.itemis.maven.plugins/cdi-plugin-utils)

This small library enables the usage of CDI-based dependency injection in Apache Maven plugins which changes the way of implementing Maven plugins fundamentally.


Requirements
------------
* JDK 1.7 or higher
* Apache Maven 3.x


The Idea Behind It
------------------
The explicit desire for dependency injection (DI) in Maven plugins came up during the development of the [Unleash Maven Plugin](https://github.com/shillner/unleash-maven-plugin/). There it was necessary to dynamically inject an implementation of the ScmProvider interface into the provider registry that isn't even known at compile time. A second wish was to simply add those implementations as plugin dependencies when configuring the plugin for your project.

Since an examination of Maven's DI capabilities did not yield any satisfactory results, this project was brought to life. First there was only the need to enable DI for Maven plugins, without any technological preference. But fast the requirements became more concrete and the choice fell on CDI based on its reference implementation Weld. Here are some of the core requirements that led to the current concepts and implementation:

*   DI in general to decouple components
*   Classpath-based autowiring of components
*   Distributed feature implementation without having to grind parameters through hundreds of classes
*   Ensuring extensibility of the plugins based on this library
*   Safe execution of the various processing steps of the plugin with implicit rollback in case of an error


The Core Concepts
-----------------
*   CDI-based dependency injection implemented using [Weld SE](https://docs.jboss.org/weld/reference/latest/en-US/html/environments.html#_java_se)
   *   @Inject
   *   Qualifiers, Alternatives
   *   Producers
   *   @PostConstruct, @PreDestroy
   *   Events
*   Classpath-based autowiring
   *   The plugin's classpath is automatically scanned for beans (plugin and plugin dependencies)
   *   No need for declaring beans in any kind of descriptor or manually bind or wire beans
*   Workflow-based architecture
   *   Plugins define a default workflow for each Mojo
   *   Workflow consisting of several processing steps which results in much smaller and clearer feature implementations
*   Safe workflow processing
   *   The plugin processes the workflow step by step
   *   Steps can implement one or more rollback methods that are called under certain circumstances
   *   If any workflow step fails with an exception all processed steps are rolled-back in their reverse order
   *   Each step only needs to rollback its own changes
*   Extensibility by design
   *   Classpath scanning enables you to add more processing steps or other implementations to the plugin dependencies
   *   Overriding of the default workflow of a Mojo makes it possible to redefine the workflow, f.i. when embedding new steps
   

Further Information
-------------------
For more detailed information about how to implement Maven plugins using this library please refer to the [Project Wiki](https://github.com/shillner/maven-cdi-plugin-utils/wiki). There all concepts and their implementation as well as the general usage is explained in detail.

A reference plugin that bases on this library is available here: [Unleash Maven Plugin](https://github.com/shillner/unleash-maven-plugin/)
This plugin provides f.i. an SCM provider API that is implemented in several external projects such as [Unleash SCM Provider for Git](https://github.com/shillner/unleash-scm-provider-git). These provider implementations can then be added to the plugin dependencies in order to support other SCM types during processing.

A further project is available here: [Maven CDI Processing hooks](https://github.com/shillner/maven-cdi-plugin-hooks)
This project provides some additional processing step implementations that can be used to extend processing workflows by simply adding the library to the plugin dependencies and overriding the processing workflow.
