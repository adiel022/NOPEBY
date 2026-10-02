# Proguard / R8 rules for NOPEBY

# Keep application package classes and members
-keep class ar.axt.** { *; }
-keepclassmembers class ar.axt.** { *; }

# Keep OpenGL and Android native classes
-keep class javax.microedition.khronos.** { *; }
-keep class android.opengl.** { *; }
