RUBT:
	javac -cp . -source 1.6 -target 1.6 ./src/RUBTClient.java

RUN:
	java -cp . src.RUBTClient project3.torrent test.avi

clean:
	rm ./src/*.class
	rm -f a.img test.avi test.mp3
