JFLAGS = -g -classpath ../ImageJ/ij.jar:../Jama/jama.jar 
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	  Pombe_Measurer.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class *~
