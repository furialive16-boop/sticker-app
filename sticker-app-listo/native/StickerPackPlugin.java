package com.stickersonido.app;

import android.content.Intent;
import android.util.Base64;
import com.getcapacitor.JSArray;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;

// Guarda el paquete y le pide a WhatsApp que lo agregue (API oficial de paquetes de stickers).
@CapacitorPlugin(name = "StickerPack")
public class StickerPackPlugin extends Plugin {
  private void write(File f, byte[] data) throws Exception {
    try (FileOutputStream o = new FileOutputStream(f)) { o.write(data); }
  }

  @PluginMethod
  public void addPack(PluginCall call) {
    try {
      String id = call.getString("id", "mis_stickers");
      String name = call.getString("name", "Mis stickers");
      String pub = call.getString("publisher", "Sticker con sonido");
      String tray = call.getString("tray");
      JSArray list = call.getArray("stickers");
      if (list == null || list.length() < 3 || tray == null) { call.reject("Se necesitan al menos 3 stickers"); return; }

      File dir = new File(new File(getContext().getFilesDir(), "stickers"), id);
      File[] old = dir.listFiles();
      if (old != null) for (File f : old) f.delete();
      dir.mkdirs();
      write(new File(dir, "tray.png"), Base64.decode(tray, Base64.DEFAULT));
      JSONArray files = new JSONArray();
      for (int i = 0; i < list.length(); i++) {
        String n = (i + 1) + "_" + System.currentTimeMillis() + ".webp";
        write(new File(dir, n), Base64.decode(list.getString(i), Base64.DEFAULT));
        files.put(n);
      }
      JSONObject j = new JSONObject();
      j.put("name", name); j.put("publisher", pub); j.put("files", files);
      j.put("version", String.valueOf(System.currentTimeMillis()));
      write(new File(dir, "pack.json"), j.toString().getBytes("UTF-8"));

      Intent i = new Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
      i.putExtra("sticker_pack_id", id);
      i.putExtra("sticker_pack_authority", getContext().getPackageName() + ".stickercontentprovider");
      i.putExtra("sticker_pack_name", name);
      try {
        getActivity().startActivityForResult(i, 200);
        call.resolve();
      } catch (Exception e) {
        call.reject("WhatsApp no está instalado o es una versión antigua");
      }
    } catch (Exception e) {
      call.reject("Error al guardar el paquete: " + e.getMessage());
    }
  }
}
