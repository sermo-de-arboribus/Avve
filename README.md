# Avve overview
Avve is a little toolbox for pre-processing e-books in EPUB format in order to use the e-book data in a [WEKA](https://www.cs.waikato.ac.nz/~ml/weka/) machine learning application.  
Avve produces output in XRFF format, which can be consumed by the Weka data mining applications, tools, and libraries, as well as being worked on with XML tools.  
It is written in Java, handles dependencies via Maven, and is currently unfinished work-in-progress.

# Installation
To install the programs means to build a runnable .jar file. You need a Java JDK and Maven installed in your system. In order to execute lemmatization of the e-book text, you will also need to install the [TreeTagger](http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/). Note that TreeTagger's licence only allows you personal or academic usage, not commercial.  
Download the sources, go to the main folder "Avve" and enter `mvn package`. You'll find an executable .jar file in the "target" subfolder, it's called `avve-1.0-jar-with-dependencies.jar`.  

# Running Avve
You can run Avve from the command line with a statement like this:  
`java [Systemproperties] -jar avve-1.0-jar-with-dependencies.jar [Parameters]`  
In the system properties you should at least set the path to your TreeTagger installation directory, e.g. `-Dtreetagger.home=C:\TreeTagger`. The most important parameter is `-folder`. This points to a directory where EPUB files are stored in subfolders. The subfolder names are representing the classes, that WEKA should learn. So, if you want to train a machine learning algorithm to distinguish between fiction and non-fiction books, you would put all fiction example EPUB in a subfolder named "fiction" and all others into a folder called "non-fiction".
There are further command line arguents available, which are to be documented still. For the time being you could run Avve with an invalid argument, e.g. "foobar" and you will get an error message with a list and short description of the available options.
