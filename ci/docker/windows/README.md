### Build image

```
docker build -t skiko-build-windows-ltsc2022-amd64:latest -m 2G .
```

### Run container

```
docker run -it skiko-build-windows-ltsc2022-amd64:latest 
```

* To customize memory constraints, use `-m` argument (e.g. `-m 2G`)
* To customize number of available CPU cores, use `--cpus` argument (e.g. `--cpus=2`)

### Publish image to Compose repo

```
docker login public.registry.jetbrains.space
docker tag skiko-build-windows-ltsc2019-amd64:latest public.registry.jetbrains.space/p/compose/docker/skiko-build-windows-ltsc2022-amd64:latest
docker push public.registry.jetbrains.space/p/compose/docker/skiko-build-windows-ltsc2022-amd64:latest
```