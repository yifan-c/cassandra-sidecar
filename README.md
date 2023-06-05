# Apache Cassandra Sidecar [WIP]

This is a Sidecar for the highly scalable Apache Cassandra database.
For more information, see [the Apache Cassandra web site](http://cassandra.apache.org/) and [CIP-1](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=95652224).

**This is project is still WIP.**

Requirements
------------
  1. Java >= 1.8 (OpenJDK or Oracle), or Java 11
  2. Apache Cassandra 4.0.  We depend on virtual tables which is a 4.0 only feature.
  3. [Docker](https://www.docker.com/products/docker-desktop/) for running integration tests.

Getting started: Running The Sidecar
--------------------------------------

After you clone the git repo, you can use the gradle wrapper to build and run the project. Make sure you have 
Apache Cassandra running on the host & port specified in `conf/sidecar.yaml`.

    $ ./gradlew run
  
Configuring Cassandra Instance
------------------------------

While setting up cassandra instance, make sure the data directories of cassandra are in the path stored in sidecar.yaml file, else modify data directories path to point to the correct directories for stream APIs to work.

Testing
-------

We rely on the in-jvm dtest framework for testing. You must manually build the dtest jars before you start integration tests.
At the moment, the JMX feature is unreleased in Cassandra, so you can use the following to build from the PR branches:

```shell
./scripts/build-dtest-jars.sh
```

The build script supports two parameters:
- `REPO` - the Cassandra git repository to use for the source files. This is helpful if you need to test with a fork of the Cassandra codebase.
  - default: `git@github.com:apache/cassandra.git`
- `BRANCHES` - a space-delimited list of branches to build.
  -default: `"cassandra-3.11 cassandra-4.1"`


Optionally, if you run everything under JDK8 you can also build 3.11 - just add the branch `jmx-in-jvm-dtest-3.11`
Remove any versions you may not want to test with. We recommend at least the latest (released) 4.X series and `trunk`.

The test framework is set up to run 3.11 and 4.1 (Trunk) tests (see `TestVersionSupplier.java`) by default.  
You can change this via the Java property `cassandra.sidecar.versions_to_test` by supplying a comma-delimited string.
For example, `-Dcassandra.sidecar.versions_to_test=3.11,4.0,4.1,5.0`.

In order for tests to run successfully under JDK11, you'll need to add the following JVM arguments to your test runner of choice.
You should also set your test framework to fork a new process at least every class, if not every method, as there are still
a few unresolved memory-related issues in the in-jvm dtest framework.
```
-Djdk.attach.allowAttachSelf=true
-XX:+UseConcMarkSweepGC
-XX:+CMSParallelRemarkEnabled
-XX:SurvivorRatio=8
-XX:MaxTenuringThreshold=1
-XX:CMSInitiatingOccupancyFraction=75
-XX:+UseCMSInitiatingOccupancyOnly
-XX:CMSWaitDuration=10000
-XX:+CMSParallelInitialMarkEnabled
-XX:+CMSEdenChunksRecordAlways
--add-exports
java.base/jdk.internal.misc=ALL-UNNAMED
--add-exports
java.base/jdk.internal.ref=ALL-UNNAMED
--add-exports
java.base/sun.nio.ch=ALL-UNNAMED
--add-exports
java.management.rmi/com.sun.jmx.remote.internal.rmi=ALL-UNNAMED
--add-exports
java.rmi/sun.rmi.registry=ALL-UNNAMED
--add-exports
java.rmi/sun.rmi.server=ALL-UNNAMED
--add-exports
java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED
--add-exports
java.sql/java.sql=ALL-UNNAMED
--add-opens
java.base/java.lang.module=ALL-UNNAMED
--add-opens
java.base/java.net=ALL-UNNAMED
--add-opens
java.base/jdk.internal.loader=ALL-UNNAMED
--add-opens
java.base/jdk.internal.ref=ALL-UNNAMED
--add-opens
java.base/jdk.internal.reflect=ALL-UNNAMED
--add-opens
java.base/jdk.internal.math=ALL-UNNAMED
--add-opens
java.base/jdk.internal.module=ALL-UNNAMED
--add-opens
java.base/jdk.internal.util.jar=ALL-UNNAMED
--add-opens
jdk.management/com.sun.management.internal=ALL-UNNAMED
-Dcassandra-foreground=yes
-Dcassandra.config=file:///Users/drohrer/p/apache-cassandra-bare/cassandra-4.1/conf/cassandra.yaml
-Dcassandra.jmx.local.port=7199
-Dcassandra.logdir=/Users/drohrer/p/apache-cassandra-bare/cassandra-4.1/data/logs
-Dcassandra.reads.thresholds.coordinator.defensive_checks_enabled=true
-Dcassandra.storagedir=/Users/drohrer/p/apache-cassandra-bare/cassandra-4.1/data
-Dcassandra.triggers_dir=/Users/drohrer/p/apache-cassandra-bare/cassandra-4.1/conf/triggers
-Djava.library.path=/Users/drohrer/p/apache-cassandra-bare/cassandra-4.1/lib/sigar-bin
-Dlogback.configurationFile=file:///Users/drohrer/p/apache-cassandra-bare/cassandra-4.1/conf/logback.xml
-Xmx2G
-Xms2G
-ea
```

CircleCI Testing
-----------------

You will need to use the "Add Projects" function of CircleCI to set up CircleCI on your fork.  When promoted to create a branch, 
do not replace the CircleCI config, choose the option to do it manually.  CircleCI will pick up the in project configuration.

Contributing
------------

We warmly welcome and appreciate contributions from the community. Please see [CONTRIBUTING.md](CONTRIBUTING.md)
if you wish to submit pull requests.

Wondering where to go from here?
--------------------------------
  * Join us in #cassandra on [ASF Slack](https://s.apache.org/slack-invite) and ask questions 
  * Subscribe to the Users mailing list by sending a mail to
    user-subscribe@cassandra.apache.org
  * Visit the [community section](http://cassandra.apache.org/community/) of the Cassandra website for more information on getting involved.
  * Visit the [development section](http://cassandra.apache.org/doc/latest/development/index.html) of the Cassandra website for more information on how to contribute.
  * File issues with our [Sidecar JIRA](https://issues.apache.org/jira/projects/CASSANDRASC/issues/)
