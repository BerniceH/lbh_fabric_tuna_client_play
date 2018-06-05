name := "lbh_fabric_client_play"
 
version := "1.0" 
      
lazy val `lbh_fabric_client_play` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.2"

libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice )


/*
*
* Step1 下載Client SDK
* 開發Hyperledger-Fabric
*
* */
libraryDependencies ++= Seq(
  "org.hyperledger.fabric-sdk-java" % "fabric-sdk-java" % "1.1.0"
)

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

      