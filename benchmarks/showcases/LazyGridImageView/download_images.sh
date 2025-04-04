#!/bin/bash

TARGET_DIR="composeApp/src/commonMain/composeResources/drawable"
TARGET_DIR_IOS="nativeiosApp/nativeiosApp/Assets.xcassets"
TRAGET_DIR_ANDROID="nativeAndroidApp/app/src/main/assets/drawable"

mkdir -p "$TARGET_DIR"
mkdir -p "$TARGET_DIR_IOS"
mkdir -p "$TARGET_DIR_ANDROID"

echo "Downloading 1000 images from picsum.photos..."

# Download 1000 images
for i in $(seq 1 1000); do
    imagename="image$(printf "%03d" $i)" 
    filename="${imagename}.jpg"
    filepath="$TARGET_DIR/$filename"

    echo "Downloading image $i/1000: $filename"

    # Use a specific image ID to ensure we get different images
    # picsum.photos has images with IDs from 0 to about 1084
    # We'll use modulo to cycle through available IDs if we need more than available
    image_id=$((i % 1085))

    # Download the image with specific ID, size 512x512, in JPG format
    curl -s -L -o "$filepath" "https://picsum.photos/id/$image_id/512/512.jpg"

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
        imageset_dir="$TARGET_DIR_IOS/${imagename}.imageset"
        
        mkdir -p "$imageset_dir"

        cp $filepath $imageset_dir
        echo "Image $filename copied to $imageset_dir" 

        # Create Contents.json file for the imageset with only 1x scale
        cat > "$imageset_dir/Contents.json" << EOF
{
  "images" : [
    {
      "idiom" : "universal",
      "scale" : "1x",
      "filename" : "${filename}"
    }
  ],
  "info" : {
    "version" : 1,
    "author" : "xcode"
  }
}
EOF
        cp $filepath $TRAGET_DIR_ANDROID
        echo "Image $filename copied to $TARGET_DIR_ANDROID"
    fi

    # Add a small delay to avoid overwhelming the server
    sleep 0.2
done

echo "Download complete. Successfully downloaded images are in $TARGET_DIR, $TARGET_DIR_IOS, $TARGET_DIR_ANDROID"

