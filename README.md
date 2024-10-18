# Fesse de Bouc

## What is it?

I just got too annoyed at the need of a Facebook account to know what was happening in different important groups like
class group of my kids for social events outside the school basically to create event like birthday parties and chat,
so I decided to see if I could create an alternative and here it is.

## Technology

This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop, Server.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - `commonMain` is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* `/server` is for the Ktor server application and uses Exposed as ORM.

* `/shared` is for the code that will be shared between all targets in the project.
  The most important subfolder is `commonMain`. If preferred, you can add code to the platform-specific folders here
  too.

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

You can open the web application by running the `:composeApp:wasmJsBrowserDevelopmentRun` Gradle task.

## Requirements

- An IDE supporting Kotlin Multiplatform (Intellij or Fleet)
- Android Studio
- XCode
- Java 21
- A postgres DB - I run it from Docker with:
  `docker run -d --name fessedebouc_postgres -e POSTGRES_PASSWORD=mysecretpassword -e POSTGRES_DB=fessedebouc -p 5432:5432  postgres:16-alpine`

## Running the server

You will need the `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` env to be able to log in. The server will not start
without them.

You can start it from your IDE from the main ApplicationKt file then run `../gradlew -t build -x test -i` in the server
folder to auto-reload changes. Auto-reload will not create new tables and load new dependencies. You will need to
restart the server for this.

Tables will be created or updated (only add column) when the application starts.

Now the server is accessible from http://localhost:8080/

After log in, you will get the access token from the callback endpoint that you can use as Bearer Token.
