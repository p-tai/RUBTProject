RUBT:
	javac -cp . -source 1.6 -target 1.6 RUBTClient.java

IMG:
	java RUBTClient project2.torrent a.img

RUN:
	java RUBTClient project3.torrent test.avi

clean:
	rm *.class
	rm -f a.img test.avi test.mp3
