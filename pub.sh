rm -rf build
docker run --rm -it -v /root/github/nats.java:/opt/nats.java al-gradle bash -c "cd /opt/nats.java; gradle -x test build"
docker build . --no-cache -t iscr.io:5002/nats-test
docker push iscr.io:5002/nats-test 
