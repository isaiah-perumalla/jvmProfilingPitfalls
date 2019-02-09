#/bin/bash
for i in {1..10} 
do
	nc -W20 localhost 9090 &
done
