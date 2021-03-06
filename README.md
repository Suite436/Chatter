# Chatter
Chatter (working title) is a forum for users to post about anything they choose. Users will see posts from other users who share something in common with them, thereby creating an automatically-curated feed of live posts that might interest each user.

## Configuration
### Maven
###### Installation
This project supports the use of [Apache Maven](https://maven.apache.org/) for compiling source and running unit tests.
The installation of Apache Maven is a simple process of extracting the archive and adding the bin folder with the mvn command to the PATH.
See documentation [here](https://maven.apache.org/install.html).

Apache Maven may also be installed easily using [Homebrew](http://brew.sh/): ```$ brew install maven```

###### Compilation
After following the installation instructions, navigate to the base project directory and run
```
$ mvn compile
```

###### Unit Tests
After following the installation instructions, navigate to the base project directory and run
```
$ mvn test
```

###### Running the CLI
After following the installation instructions, navigate to the base project directory and run
```
$ mvn compile exec:java
```
This will build any recent changes and then start the interactive CLI for Chatter.

###### Using the AWS clients
To use any of the AWS clients built into Chatter, please follow the instructions for setting up and using AWS credentials here:
http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/credentials.html
