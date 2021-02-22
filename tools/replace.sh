OLDVER=0.3.0-rc1
NEWVER=0.3.0
find -E ../  -regex '.*\.(kts|properties|kt)' -exec sed -i '' -e "s/$OLDVER/$NEWVER/g" {} \; 
