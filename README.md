# feb-maven-plugin
[![Build Status](https://travis-ci.org/bluepack/feb-maven-plugin.svg?branch=master)](https://travis-ci.org/bluepack/feb-maven-plugin)

Maven Plugin for deploying IBM Forms Experience Builder applications using the Application Management REST API

## Goals

### feb-deploy
Deploys a nitro_s file to IBM Forms Experience Builder.  

#### Installation

As this is a typical Maven plugin, simply declare the plugin in the `<plugins>` section of your POM file:

```xml
<plugins>
    <plugin>
        <groupId>bluepack.feb</groupId>
        <artifactId>feb-maven-plugin</artifactId>
        <version>{release-version}</version>
        <configuration>
            <appUid>e2bd8de2-6303-4b85-83be-23ca5788ad2e</appUid>
            <filename>{path-to-nitro-file}</filename>
            ...
        </configuration>
    </plugin>
</plugins>
```
#### Usage
You can deploy a project with the above configurations by running this command (assumes you're levering a settings.xml to manage FEB credentials)

    mvn clean package bluepack.feb:feb-maven-plugin:1.1:feb-deploy --global-settings path/to/settings.xml

You can also deploy without a settings.xml file and passing the configuration directly

    mvn clean package bluepack.feb:feb-maven-plugin:1.1:feb-deploy -Dfeb.username=user -Dfeb.password=password

Plugin also supports running it without a project / pom.xml

    mvn bluepack.feb:feb-maven-plugin:1.1:feb-deploy -Dfeb.appUid=GUID-HERE -Dfeb.filename=/path/to/nitro/file -Dfeb.username=user -Dfeb.password=password --global-settings path/to/settings.xml

#### Special Notes - Nitro File Generation
IBM Forms Experience Builder's nitro_s files are essentially zip files.  However in order to deploy these zip files, 
the files must end with the `.nitro_s` file extension.  If you were to leverage the maven assembly plugin to
generate zip files based on the FEB XML file(s) included in the nitro_s, it won't be deployable since the assembly
plugin does not let you specify the file extension.

To get around this problem, this plugin defines a maven assembly plugin extension that you can use in your project to
generate nitro_s files.  Below is a sample assembly package.xml and plugin definition needed in your pom.xml file.

src/main/assembly/package.xml
```xml
<assembly
	xmlns="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.2"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.2 http://maven.apache.org/xsd/assembly-1.1.2.xsd">
	<id>nitro</id>
	<includeBaseDirectory>false</includeBaseDirectory>

	<formats>
		<format>nitro_s</format>
	</formats>
	
	<fileSets>
		<fileSet>
			<directory>src/main/resources</directory>
			<outputDirectory>/</outputDirectory>
		</fileSet>
	</fileSets>
</assembly>
```

pom.xml plugins section
```xml
<plugins>
    ...
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-assembly-plugin</artifactId>
        <version>2.2</version>
        <dependencies>
            <dependency>
                <groupId>bluepack.feb</groupId>
                <artifactId>feb-maven-plugin</artifactId>
                <version>{release-version}</version>
            </dependency>
        </dependencies>
        <executions>
            <execution>
                <phase>package</phase>
                <goals>
                    <goal>single</goal>
                </goals>
                <configuration>
                    <descriptor>src/main/assembly/package.xml</descriptor>
                    <appendAssemblyId>false</appendAssemblyId>
                    <finalName>${project.artifactId}</finalName>
                </configuration>
            </execution>
        </executions>
    </plugin>
</plugins>
```

#### Configuration options
-------------------------
+ **appUid** `String` - [Required] The universal ID of the FEB application. System property: *feb.appUid*.
+ **filename** `File` - [Required] Path to the nitro_s file that will be deployed. System property: *feb.filename*.
+ **hostname** `String` - The hostname of the FEB instance where the form will be deployed. System property: *feb.hostname*. Default value is: *localhost*
+ **port** `Integer` - The HTTP port of the FEB server, if unspecified the value will be 80 or 443 based on the useSSL parameter. System property: *feb.port*. Default value is: *localhost*
+ **useSSL** `Boolean` - [Not Implemented Yet] If true it will use HTTPS when connecting to the FEB server. System property: *feb.useSSL*. Default value is: *false*
+ **accessType** `Enum` - FEB security mode. System property: *feb.accessType*. Options: `secure`, `anon`. Default value is: *secure*
+ **username** `String` - FEB Administrator username. If not given, it will be looked up through `settings.xml`'s server with ${settingsKey} as key. System property: *feb.username*.
+ **password** `String` - FEB Administrator password. If not given, it will be looked up through `settings.xml`'s server with ${settingsKey} as key. System property: *feb.password*.
+ **settingsKey** `String` - Server's *id* in `settings.xml` to look up username and password. Defaults to ${hostname} if not given.
+ **replaceSubmittedData** `Enum` - Turn on to replace the existing submission data with the data in the uploaded application. System property: *feb.replaceSubmittedData*. Options: `on`, `off`. Default value is: *off*
+ **deploy** `Boolean` - If true automatically deploys the application as part of the import.. System property: *feb.deploy*. Default value is: *true*
+ **importData** `Boolean` - If true imports the submission data included within the form. System property: *feb.importData*. Default value is: *false*
+ **cleanIds** `Boolean` - If true removes all groups and users from roles within the imported application ensuring that only the current authenticated user has access to the application. System property: *feb.cleanIds*. Default value is: *false*

