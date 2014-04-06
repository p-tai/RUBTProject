RUBT:
	javac -cp . -source 1.6 -target 1.6 RUBTClient.java

IMG:
	java RUBTClient project2.torrent a.img

RUN:
	java RUBTClient project2.torrent test.mp3

clean:
	rm *.class
	rm -f a.img
