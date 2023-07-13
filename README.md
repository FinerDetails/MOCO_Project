# MOCO_Project

The idea of the game is that the user has to search for mushrooms to survive increasing hunger in two different modes: a map and augmented reality.
By using the map the user has to move by walking and collect found mushrooms by clicking on them, in order to increase the hunger meter,
depicted by an image of a mushroom.
Mushrooms appear by getting to a close enough radius to them.
Exiting the radius will make the mushrooms disappear and become unclickable.
These hidden mushrooms can only be found in a purple area referred to as "the zone". The use of Ar is only possible within the zone. To enter Ar,
click the appearing switch on the left upper corner.
The anchors for the mushrooms exist in the Ar mode, but interaction or models don't work.
Collecting many mushrooms can cause the meter to go over its limit, which is intended by design.
However, no proper ending has been added after the meter reaches below zero.
The meter will keep decrementing until the game is closed.


The app consists of 3 activities: MainActivity, ArActivity and MapActivity

MainActivity was meant for the main menu, but is currently used just for launching MapActivity.
The user is able to switch between MapActivity and ArActivity to interract with the game differently.
MapActivity uses Google Maps API, while ArActivity utilizes Googles ArCore API.
Data is shared via the GameData Class where changes to game objects are recorded.

The MapActivity takes advantage of Googles updated map renderer that allows the app to use a customized cloud based map,
instead of the default map.
The app requires and prompts the use of fine location data and the use of camera.


TESTING THE APP

The latest code is inside the AR branch.
To get the game should run without any changes, but it's good to
ensure that the maps key is in gradle.properties file and check that the following code is inside the build.gradle file:

"Properties properties = new Properties()
properties.load(project.rootProject.file('gradle.properties').newDataInputStream())
manifestPlaceholders = [MAPS_API_KEY: "${properties.getProperty('MAPS_API_KEY')}"]"

(key is intended to kept active for a few weeks after end of the course, unless it gets leaked further)


Using a physical device instead of an emulator is highly recommended.

Please check the file local.properties as well and change the SDK path to wherever your Android SDK is stored.

Some references used in the creation of the code:
https://developers.google.com/maps/documentation/android-sdk/map
https://developers.google.com/maps/documentation/android-sdk/location *this demo is closely followed in the code*
https://developers.google.com/maps/documentation/android-sdk/marker
https://developers.google.com/maps/documentation/android-sdk/cloud-customization/overview

The ArActivity is build around the ARCore examples shared_camera_java and geospatial_java.
Both Github repos can be found here:
https://github.com/google-ar/arcore-android-sdk/tree/master/samples/shared_camera_java
https://github.com/google-ar/arcore-android-sdk/tree/master/samples/geospatial_java




