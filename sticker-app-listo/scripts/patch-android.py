# Agrega el plugin de paquetes de stickers al proyecto Android que genera Capacitor.
import pathlib

base = pathlib.Path("android/app/src/main")
pkg = base / "java/com/stickersonido/app"
pkg.mkdir(parents=True, exist_ok=True)
for name in ["StickerPackPlugin.java", "StickerContentProvider.java"]:
    (pkg / name).write_text(pathlib.Path("native", name).read_text(encoding="utf-8"), encoding="utf-8")

(pkg / "MainActivity.java").write_text("""package com.stickersonido.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    registerPlugin(StickerPackPlugin.class);
    super.onCreate(savedInstanceState);
  }
}
""", encoding="utf-8")

m = base / "AndroidManifest.xml"
x = m.read_text(encoding="utf-8")
assert "<application" in x and "</application>" in x, "Manifiesto inesperado"
x = x.replace("<application", """<queries>
        <package android:name="com.whatsapp" />
        <package android:name="com.whatsapp.w4b" />
    </queries>
    <application""", 1)
x = x.replace("</application>", """<provider
            android:name=".StickerContentProvider"
            android:authorities="${applicationId}.stickercontentprovider"
            android:enabled="true"
            android:exported="true"
            android:readPermission="com.whatsapp.sticker.READ" />
    </application>""", 1)
m.write_text(x, encoding="utf-8")
print("Proyecto Android parchado")
