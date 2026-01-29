// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CafesitoIOS",
    platforms: [.iOS(.v14)],
    products: [
        .library(name: "Shared", targets: ["Shared"])
    ],
    targets: [
        .binaryTarget(
            name: "Shared",
            path: "Shared.xcframework"
        )
    ]
)
