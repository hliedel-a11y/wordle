
Wordle.jar: Main.class WordList.class win.wav lib/gson.jar
	jar xf lib/gson.jar com
	jar cfe Wordle.jar Main *.class com win.wav
	rm -rf com

%.class: %.java
	javac -cp .:lib/gson.jar $<

run: Wordle.jar
	java -jar Wordle.jar

clean:
	rm -rf *.class com Wordle.jar
