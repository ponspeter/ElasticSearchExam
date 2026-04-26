### How to Run the Elasticsearch Dockerfile

To run the provided `Dockerfile`, follow these steps:

#### 1. Build the Docker Image
Open your terminal in the project root directory (`C:\Users\ponsp\workspace\ElasticSearchExam`) and run:

```powershell
docker build -t elasticsearch-exam .
```
*   `-t elasticsearch-exam`: Assigns a tag (name) to your image.
*   `.`: Tells Docker to look for the `Dockerfile` in the current directory.

#### 2. Run the Container
Once the image is built, start a container using:

```powershell
docker run -d --name elasticsearch-container -p 9200:9200 -p 9300:9300 elasticsearch-exam
```
*   `-d`: Runs the container in detached mode (in the background).
*   `--name elasticsearch-container`: Gives your running container a specific name.
*   `-p 9200:9200`: Maps port 9200 on your host to port 9200 in the container (for REST API).
*   `-p 9300:9300`: Maps port 9300 for nodes communication.

#### 3. Verify it's Running
You can check if Elasticsearch is up by visiting `http://localhost:9200` in your browser or running:

```powershell
curl http://localhost:9200
```

Since the `Dockerfile` has `xpack.security.enabled=false`, you won't need a password to access it.

#### 4. Stop the Container
To stop and remove the container when you're done:

```powershell
docker stop elasticsearch-container
docker rm elasticsearch-container
```