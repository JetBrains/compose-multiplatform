# Sample with common UI between android, desktop, web, ios

In this sample all platforms use own native UIKit (except Desktop):
- Android - material Compose UI
- Desktop - material Compose UI
- Web - Compose Web
- iOS - UIKit Compose components

But all application UI implemented in `commonMain` - low level build kit elements like `Text`, `Column` etc is `expect`
and implemented on all platform by platform UI.

# Screenshots
## iOS
![ezgif-3-3924687fe7](https://user-images.githubusercontent.com/5010169/150639117-ce64ecc1-e36a-405b-aaa3-be1b2e32759e.gif) 

## Android
![ezgif-3-4ee9b0a5b6](https://user-images.githubusercontent.com/5010169/150639110-58ae9de4-449b-47a2-9aba-ae74b22dfde9.gif)

## Desktop
![ezgif-3-fc12402deb](https://user-images.githubusercontent.com/5010169/150639115-d12f893b-06aa-42c1-95ea-8dce3c3a0778.gif) 

## Web
![ezgif-3-bbc7ea0c78](https://user-images.githubusercontent.com/5010169/150639108-4a10d23c-8605-4df4-ba75-aa9b3d6ae6aa.gif)

