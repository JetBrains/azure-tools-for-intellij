### Overview

`buildspec` directory contains Dockerfiles that are used to build Docker images for different infrastructure needs. 
Images are deployed to under `jetbrainsinfra/rider-azure-toolkit` image on a Docker Hub. Docker image is used for running
a build process for Utils, Azure Toolkit plugins, and running tests. We use three corresponding tags for the same image:

- `jetbrainsinfra/rider-azure-toolkit:utils`
- `jetbrainsinfra/rider-azure-toolkit:tests`
- `jetbrainsinfra/rider-azure-toolkit:plugin`

TeamCity configurations uses one of those images to run build configuration for building Utils, running Tests, and building plugins.

**Note:** Dockerfiles here are are not included in infrastructure automation steps. It is used only to store a history about Docker image
configuration. Please keep it in ming and not forget to update Dockerfile in a repo if you update the `jetbrainsinfra/rider-azure-toolkit` 
Image in Docker Hub account.

### To update the Docker Image

If you would like to update the `jetbrainsinfra/rider-azure-toolkit` Docker Image we use on a TeamCity for building the plugin, 
please do the following:
1. Make sure Docker daemon is running
2. Update the corresponding Dockerfile under `buildspec` directory
3. Navigate to this Dockerfile location in terminal 
4. Run: `docker build jetbrainsinfra/rider-azure-toolkit:<tag_name>`, where `<tag_name>` is one of the corresponding tag names (`utils`, `tests`, `plugin`)
5. Wait for image to be built locally (may take some time)
6. Push image to Docker Hub: `docker push jetbrainsinfra/rider-azure-toolkit:<tag_name>`. Please note, you should have access to push images to 
   `jetbrainsinfra/rider-azure-toolkit` Docker Hub image. 