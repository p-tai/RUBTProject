RUBT:
	javac -cp . -source 1.6 -target 1.6 RUBTClient.java

IMG:
	java RUBTClient project1.torrent a.img

clean:
	rm *.class
	rm a.img
