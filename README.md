TNSSYNC
----
TNSSYNC creates tnsnames.ora file for services listed in a config file (tnssync.ora) getting Oracle Net Description data from a directory server (described in ldap.ora)

Usage
----

```
usage: java -jar tnssync.jar [-ta <DIR>] [-l <FILE>]
usage: java -jar tnssync.jar -h
usage: java -jar tnssync.jar -v

 -h,--help                         Print this message
 -l,--logback_config_file <FILE>   Logback configuration file (default file: TNS_ADMIN_DIR/tnssync_logback.xml)
 -ta,--tns_admin_dir <DIR>         Specifies a directory where the SQL*Net configuration files (like sqlnet.ora,
                                   ldap.ora and tnsnames.ora) are located. Configuration file for this program
                                   (tnssync.ora) is also found here.
 -v,--version                      Print the version of the application
```

Logging
----
tnssync provides logging functionality using Simple Logging Facade for Java (SLF4J) with a logback backend.

Warning
----
tnssync is able to owerwrite / append the existing tnsnames.ora file!

License
-------
Copyright 2016 Attila Lerant, aleerantdev@gmail.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.