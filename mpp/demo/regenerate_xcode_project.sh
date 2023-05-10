# Script to regenerate xcode project

# make script folder current or exit
cd "$(dirname "$0")" || exit

if command -v xcodegen >/dev/null 2>&1; then
  # xcodegen exists

  projPath="ComposeDemo.xcodeproj"

  if [ -d "$projPath" ]; then
    echo "Removing existing project"
    rm -rf "$projPath"
  fi

  xcodegen
  open $projPath
else
  # xcodegen does not exist
  echo "Error: xcodegen not found. Please install it using 'brew install xcodegen'."
  exit 1
fi
