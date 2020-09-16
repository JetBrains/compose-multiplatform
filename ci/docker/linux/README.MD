### Build image

```
docker build -t skiko-build-ubuntu-1404-amd64:latest .
```

### Run container

```
docker run -it skiko-build-ubuntu-1404-amd64:latest 
```

* To customize memory constraints, use `-m` argument (e.g. `-m 2G`)
* To customize number of available CPU cores, use `--cpus` argument (e.g. `--cpus=2`)

### Publish image to Compose repo

```
docker login public.registry.jetbrains.space
docker tag skiko-build-ubuntu-1404-amd64:latest public.registry.jetbrains.space/p/compose/docker/skiko-build-ubuntu-1404-amd64:latest 
docker push public.registry.jetbrains.space/p/compose/docker/skiko-build-ubuntu-1404-amd64:latest
```