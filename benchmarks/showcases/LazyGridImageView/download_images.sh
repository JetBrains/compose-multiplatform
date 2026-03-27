#!/bin/bash

TARGET_DIR="composeApp/src/commonMain/composeResources/files"
TARGET_DIR_IOS="nativeiosApp/nativeiosApp/downloaded_images"
TARGET_DIR_ANDROID="nativeAndroidApp/app/src/main/assets/downloaded_images"

mkdir -p "$TARGET_DIR"
mkdir -p "$TARGET_DIR_IOS"
mkdir -p "$TARGET_DIR_ANDROID"

echo "Downloading 999 images from picsum.photos..."

# Download 999 images
for i in $(seq 1 999); do
    imagename="downloaded_image$(printf "%03d" $i)" 
    filename="${imagename}.jpg"
    filepath="$TARGET_DIR/$filename"

    echo "Downloading image $i/999: $filename"

    # Download the image with specific ID, size 512x512, in JPG format
    curl -f -s -L -o "$filepath" "https://picsum.photos/id/$i/512/512.jpg"

    # Check if download was successful
    if [ -s "$filepath" ]; then
        echo "  Success: Downloaded $filename"
    else
        echo "  Error: Failed to download $filename, trying random image instead"
        # If specific ID fails, try a random image
        curl -s -L -o "$filepath" "https://picsum.photos/512/512.jpg"

        if [ -s "$filepath" ]; then
            echo "  Success: Downloaded random image as $filename"
        else
            echo "  Error: Failed to download random image as $filename"
        fi
    fi     

    if [ -s "$filepath" ]; then
        cp $filepath $TARGET_DIR_IOS
        echo "Image $filename copied to $TARGET_DIR_IOS" 

        cp $filepath $TARGET_DIR_ANDROID
        echo "Image $filename copied to $TARGET_DIR_ANDROID"
    fi

    sleep 0.1
done

echo "Download complete. Successfully downloaded images are in $TARGET_DIR, $TARGET_DIR_IOS, $TARGET_DIR_ANDROID"
