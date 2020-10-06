# Java Concurrency Stress (jcstress)

The Java Concurrency Stress (jcstress) is the experimental harness and
a suite of tests to aid the research in the correctness of concurrency support
in the JVM, class libraries, and hardware.

## Usage

### Samples

In order to understand jcstress tests and maybe write your own, it might be useful
to work through the [jcstress-samples](https://github.com/openjdk/jcstress/tree/master/jcstress-samples/src/main/java/org/openjdk/jcstress/samples). The samples come in three groups:
 * **APISample** target to explain the jcstress API;
 * **JMMSample** target to explain the basics of Java Memory Model;
 * **ConcurrencySample** show the interesting concurrent behaviors of standard library.

See the test comments for run instructions. Most tests can be run like this:

     $ mvn clean verify -pl jcstress-samples -am
     $ java -jar jcstress-samples/target/jcstress.jar -t <test-name>

### Running The Existing Tests

The quickest way to start running jcstress is to use a prebuilt JAR, for example from [here](https://builds.shipilev.net/jcstress/).

     $ java -jar jcstress.jar

Run the JAR with `-h` to see available options.

Otherwise, you can build the entire test suite yourself:

     $ mvn clean verify
     $ java -jar tests-all/target/jcstress.jar

The project requires JDK 11+ to build. It can reference the APIs from
the future releases, as the jcstress harness will fail gracefully on API
mismatches, and the mismatched tests will be just skipped.


### Extending The Tests

Please consider contributing the interesting tests back.

If you want to develop a test, you are encouraged to get familiar with existing set of
tests first. You will have to have a class annotated with jcstress annotations, see the
harness API. Read up their Javadocs to understand the conditions that are guaranteed for
those tests. If you need some other test interface/harness support, please don't hesitate
to raise the issue and describe the scenario you want to test.

You are encouraged to provide the thorough explanation why particular test outcome is
acceptable/forbidden/special. Even though harness will print the debug output into the
console if no description is given.

You should have Mercurial and Maven installed to check out and build the tests.
You will need JDK 11+ to compile all the tests. Most tests are runnable on JDK 8+
afterwards.

The vast majority of jcstress tests are auto-generated. The custom/hand-written tests
usually go to `tests-custom`. This also allows building the smaller subset of tests:

    $ mvn clean verify -pl tests-custom -am
    $ java -jar tests-custom/target/jcstress.jar

### Using jcstress As Separate Dependency

If you want to use jcstress as separate dependency in your project, then you are recommended
to create the submodule with the jcstress tests, which would use jcstress libraries and build
steps.

[Maven Central](https://repo.maven.apache.org/maven2/org/openjdk/jcstress/jcstress-core/) contains
the latest releases of jcstress libraries. Using jcstress as the library requires special build configuration.
The easiest way to bootstrap the project with jcstress is to use the archetype:

    $ mvn archetype:generate \
     -DinteractiveMode=false \
     -DarchetypeGroupId=org.openjdk.jcstress \
     -DarchetypeArtifactId=jcstress-java-test-archetype \
     -DgroupId=org.sample \
     -DartifactId=test \
     -Dversion=1.0

Then you can build and use it:

    $ cd test
    $ mvn clean verify
    $ java -jar target/jcstress.jar


## Interpreting The Results

The tests are arranged so that a few threads are executing the test concurrently, sometimes
rendezvous'ing over the shared state. There are multiple state objects generated per each run.
Threads then either mutate observe that state object. Test harness is collecting statistics on
the observed states. In many cases this is enough to catch the reorderings or contract violations
for concurrent code.

The console output can be used to track progress and debugging. Ordinary users should
use generated HTML report, which has the full interpretation of the results.

Most of the tests are probabilistic, and require substantial time to catch all the cases.
It is highly recommended to run tests longer to get reliable results. Since the tests are
time-bound, the faster CPUs the machine has the more samples jcstress collects. There is
a tradeoff between the number of samples harness collects and the suite run time.
There are a few preset modes that set sensible test durations, see `-m`. Many
CIs run jcstress with `-m quick` for quicker turnaround.

Test failure does not immediately mean the implementation bug. The usual
suspects are the bugs in test infrastructure, test grading error, bugs in
hardware, or something else. Share your results, discuss them, we will figure
out what's wrong. Discuss the result on the relevant mailing lists first.

Two usual options are:
  * concurrency-interest@cs.oswego.edu: general discussion on concurrency
  * jcstress-dev@openjdk.java.net: to discuss jcstress issues

## Reporting Harness and Test Bugs

If you have the access to [JDK Bug System](https://bugs.openjdk.java.net/), please submit the bug there:
 * Project: CODETOOLS
 * Component: tools
 * Sub-component: jcstress

If you don't have the access to JDK Bug System, submit the bug report at "Issues" here, and wait for maintainers to pick that up.

## Development

jcstress project accepts pull requests, like other OpenJDK projects.
If you have never contributed to OpenJDK before, then bots would require you to [sign OCA first](http://openjdk.java.net/contribute).
Normally, you don't need to post patches anywhere else, or post to mailing lists, etc.
If you do want to have a wider discussion about jcstress, please refer to [jcstress-dev](https://mail.openjdk.java.net/mailman/listinfo/jcstress-dev).

Compile and run internal tests:

    $ mvn clean install

Run the quick tests:

    $ java -jar tests-all/target/jcstress.jar -m quick

GitHub workflow "JCStress Pre-Integration Tests" should pass on the changes. It would be triggered
for PRs. You can also trigger it manually for your branch.
