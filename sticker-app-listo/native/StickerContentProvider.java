package com.stickersonido.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

// Entrega a WhatsApp los paquetes de stickers guardados en filesDir/stickers/<id>/
public class StickerContentProvider extends ContentProvider {
  private File root() { return new File(getContext().getFilesDir(), "stickers"); }

  @Override public boolean onCreate() { return true; }

  private JSONObject pack(File dir) {
    try {
      File f = new File(dir, "pack.json");
      if (!f.exists()) return null;
      byte[] b = new byte[(int) f.length()];
      try (DataInputStream in = new DataInputStream(new FileInputStream(f))) { in.readFully(b); }
      return new JSONObject(new String(b, "UTF-8"));
    } catch (Exception e) { return null; }
  }

  @Override
  public Cursor query(Uri uri, String[] p, String s, String[] a, String o) {
    List<String> seg = uri.getPathSegments();
    if (seg.isEmpty()) return null;
    if (seg.get(0).equals("metadata")) {
      MatrixCursor c = new MatrixCursor(new String[]{
        "sticker_pack_identifier", "sticker_pack_name", "sticker_pack_publisher", "sticker_pack_icon",
        "android_play_store_link", "ios_app_download_link", "sticker_pack_publisher_email",
        "sticker_pack_publisher_website", "sticker_pack_privacy_policy_website",
        "sticker_pack_license_agreement_website", "image_data_version",
        "whatsapp_will_not_cache_stickers", "animated_sticker_pack"});
      File[] dirs = seg.size() > 1 ? new File[]{new File(root(), seg.get(1))} : root().listFiles();
      if (dirs != null) for (File d : dirs) {
        JSONObject j = pack(d);
        if (j == null) continue;
        c.addRow(new Object[]{d.getName(), j.optString("name"), j.optString("publisher"), "tray.png",
          "", "", "", "", "", "", j.optString("version"), 0, 1});
      }
      return c;
    }
    if (seg.get(0).equals("stickers") && seg.size() > 1) {
      MatrixCursor c = new MatrixCursor(new String[]{"sticker_file_name", "sticker_emoji", "sticker_accessibility_text"});
      JSONObject j = pack(new File(root(), seg.get(1)));
      if (j != null) {
        JSONArray files = j.optJSONArray("files");
        for (int i = 0; files != null && i < files.length(); i++)
          c.addRow(new Object[]{files.optString(i), "\uD83D\uDD0A", "Sticker"});
      }
      return c;
    }
    return null;
  }

  @Override
  public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
    List<String> seg = uri.getPathSegments();
    if (seg.size() == 3 && seg.get(0).equals("stickers_asset")) {
      String id = seg.get(1), name = seg.get(2);
      if (id.contains("..") || name.contains("..") || name.contains("/")) throw new FileNotFoundException();
      File f = new File(new File(root(), id), name);
      if (!f.exists()) throw new FileNotFoundException();
      return new AssetFileDescriptor(ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY), 0, AssetFileDescriptor.UNKNOWN_LENGTH);
    }
    throw new FileNotFoundException();
  }

  @Override
  public String getType(Uri uri) {
    List<String> seg = uri.getPathSegments();
    String auth = uri.getAuthority();
    if (seg.isEmpty()) return null;
    switch (seg.get(0)) {
      case "metadata": return "vnd.android.cursor." + (seg.size() > 1 ? "item" : "dir") + "/vnd." + auth + ".metadata";
      case "stickers": return "vnd.android.cursor.dir/vnd." + auth + ".stickers";
      case "stickers_asset": return seg.get(seg.size() - 1).endsWith(".png") ? "image/png" : "image/webp";
    }
    return null;
  }

  @Override public Uri insert(Uri u, ContentValues v) { throw new UnsupportedOperationException(); }
  @Override public int delete(Uri u, String s, String[] a) { throw new UnsupportedOperationException(); }
  @Override public int update(Uri u, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
