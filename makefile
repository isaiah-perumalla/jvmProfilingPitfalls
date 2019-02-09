FIND="find"
SOURCES_DIR="src"
MAIN_CLASS=Server
JAVA_HOME=/usr/lib/jvm/java-8-oracle
JAVA=$(JAVA_HOME)/bin/java
BUILD_DIR=build/classes
NETTY_JAR="lib/netty-all-4.1.33.Final.jar"
CLASSPATH=$(NETTY_JAR)
RM := rm -rf 
JVM_ARGS=-XX:+UnlockDiagnosticVMOptions

all_javas := ./all.javas
$(shell mkdir -p $(BUILD_DIR))

compile: $(all_javas)
	javac -classpath $(CLASSPATH) -d $(BUILD_DIR) @$<

.INTERMEDIATE: $(all_javas)
$(all_javas):
	$(FIND) $(SOURCE_DIR) -name '*.java' > $@

print_jvm_args:
	@echo $(JVM_ARGS)
classpath:
	@echo CLASSPATH='$(CLASSPATH)'

clean:
	$(RM) $(BUILD_DIR)

run: compile
	$(JAVA) $(JVM_ARGS) -cp $(BUILD_DIR):$(CLASSPATH) $(MAIN_CLASS)
run_io: compile
	$(JAVA) $(JVM_ARGS) -Dio=true -cp $(BUILD_DIR):$(CLASSPATH) $(MAIN_CLASS)
