OLDVER=0.0.0-unmerged-build39
NEWVER=0.3.0-rc1
find -E ../  -regex '.*\.(kts|properties|kt)' -exec sed -i '' -e "s/$OLDVER/$NEWVER/g" {} \; 
