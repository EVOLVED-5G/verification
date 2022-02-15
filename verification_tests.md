# Verification Tests

## Test Folder

Add this folder to the main Dummy-netapp directory, where the docker-compose.yml is located.  

## Docker-composer.yml

Modify the docker-composer.yml and add as a service the code below:

```
robot-framework:
    container_name: dummy-netapp_robot
    build:
      context: .
      dockerfile: ./tests/tools/Dockerfile
    volumes:
      - ./tests:/opt/robot-tests/tests
      - ./pythonnetapp:/opt/robot-tests/pythonnetapp
    environment:
      - REDIS_HOST=redis
      - REDIS_PORT=6379

```

## Run tests

Guide in the /tests/tests directory. There, you'll see *dummy-tests.robot*.
Use the following command to run the tests:  
```robot --outputdir ../results dummy-tests.robot```
