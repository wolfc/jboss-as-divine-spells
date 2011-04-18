# Introduction

With jboss-as-divine-spells you can query a 'broken' state of JBoss
Application Server 7 and get a condensed report.

The report will show which service is responsible for the breakage and how.
Either it is missing or has failed to start up.

# Usage

java -jar jboss-as-divine-spells.jar <pid>

Where <pid> is the process id of a running JBoss AS 7 instance.

## Fine-print

Divine spells uses the JMX Management capabilities of AS 7. For this purpose
AS 7 needs to run with the [Java Management Agent] [1]. AS 7 can be started with
the Management Agent or Divine Spells will automatically try to attach it via
the [Attach API] [2]. For the Attach API to work tools.jar must be present on
the class path.

[1]: http://download.oracle.com/javase/6/docs/technotes/guides/management/agent.html "Management Agent"
[2]: http://download.oracle.com/javase/6/docs/technotes/guides/attach/index.html "Attach API"
