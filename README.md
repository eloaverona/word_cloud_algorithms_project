## A Word Cloud Generator
##### A project for the Algorithms Design and Analysis Class at Macalester College Spring 2017

This project was inspired by Jonathan Feinberg's work with Wordle, an online word cloud generator. Feiberg wrote a paper about his work with Wordle and the algorithm he used, which can be found [here]( http://static.mrfeinberg.com/bv_ch03.pdf).

Typical word clouds display the most frequent words in a text, with the size of each word varying according to its frequency in the text. The most expensive part of the algorithm is to place the word in the cavas so that there is no overlap. I implemented three different placement strategies of varying complexity.

This program takes as input a text file and outputs an image file with a word cloud of the text input. It was developed in Java and a Java compiler is necessary to run it.

To run the project, download it or clone this project to your machine. In the directory with the source files run in the command line:

```
javac WordCloudMaker.java
```
This command will compile the code. Now, we are ready to create word clouds. There are three available types of word clouds.



### Random Placement
In this type of cloud, the words are placed randomly on the canvas.

![Random Placement](https://s2.postimg.org/nf0k2kvt5/word_cloud_17.png)

##### Text Source: [Macalester College Strategic Plan (2015)](https://www.macalester.edu/president/wp-content/uploads/sites/15/StrategicPlanningReportFinal.pdf)


To produce a random placement word cloud run:
```
java random path/to/source/file.txt path/to/output/dir/
```

### Linear Not Ordered
In this type of word cloud, the words are placed side by side, in random order.

![Linear nor ordered world cloud](https://s2.postimg.org/n3j3p8f6x/word_cloud_28.png)

##### Text Source: [The Picture of Dorian Gray by Oscar Wilde - Retrieved from Project Gutenberg](https://www.gutenberg.org/ebooks/174)

To produce a linear, but not ordered word cloud run:
```
java linear-not-ordered path/to/source/file.txt path/to/output/dir/
```


### Linear
In this type of word cloud, the words are placed side by side, ordered from most common word to least common word.

![Linear world cloud](https://s2.postimg.org/5q8tadc61/word_cloud_29.png)

##### Text Source: [The Constitution of the United States](http://constitutionus.com/)

To produce a linear word cloud run:
```
java linear path/to/source/file.txt path/to/output/dir/
```



More examples of output images can be found in the sample_output folder.
Sample input texts can be found in the sample_input folder.

Happy world cloud making!
