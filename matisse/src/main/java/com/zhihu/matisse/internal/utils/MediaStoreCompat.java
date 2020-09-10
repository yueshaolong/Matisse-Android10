/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.utils;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.fragment.app.Fragment;
import androidx.core.content.FileProvider;
import androidx.core.os.EnvironmentCompat;

import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MediaStoreCompat {

    private final WeakReference<Activity> mContext;
    private final WeakReference<Fragment> mFragment;
    private       CaptureStrategy         mCaptureStrategy;
    private       Uri                     mCurrentPhotoUri;
    private       String                  mCurrentPhotoPath;
    private String defaultPath;

    public MediaStoreCompat(Activity activity) {
        mContext = new WeakReference<>(activity);
        mFragment = null;
    }

    public MediaStoreCompat(Activity activity, Fragment fragment) {
        mContext = new WeakReference<>(activity);
        mFragment = new WeakReference<>(fragment);
    }

    /**
     * Checks whether the device has a camera feature or not.
     *
     * @param context a context to check for camera feature.
     * @return true if the device has a camera feature. false otherwise.
     */
    public static boolean hasCameraFeature(Context context) {
        PackageManager pm = context.getApplicationContext().getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    public void setCaptureStrategy(CaptureStrategy strategy) {
        mCaptureStrategy = strategy;
    }

    public void dispatchCaptureIntent(Context context, int requestCode) {
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (captureIntent.resolveActivity(context.getPackageManager()) != null) {

            //TODO 适配Android10，小于等于9使用FileProvider.getUriForFile()获取uri
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                mCurrentPhotoUri = createImageUri();
            } else {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (photoFile != null) {
                    mCurrentPhotoPath = photoFile.getAbsolutePath();
                    System.out.println("========"+mCurrentPhotoPath);
                    mCurrentPhotoUri = FileProvider.getUriForFile(mContext.get(),
                            mCaptureStrategy.authority, photoFile);
                }
            }

            System.out.println("uri========"+mCurrentPhotoUri);
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoUri);
            captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                List<ResolveInfo> resInfoList = context.getPackageManager()
                        .queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    context.grantUriPermission(packageName, mCurrentPhotoUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
            if (mFragment != null) {
                mFragment.get().startActivityForResult(captureIntent, requestCode);
            } else {
                mContext.get().startActivityForResult(captureIntent, requestCode);
            }
        }
    }

    /**
     * 创建图片地址uri,用于保存拍照后的照片 Android 10以后使用这种方法
     * @return 图片的uri
     */
    private Uri createImageUri() {
        Uri uri;
        if (mCaptureStrategy.isPublic) {
            //设置保存参数到ContentValues中
            ContentValues contentValues = new ContentValues();
            //设置文件名
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, System.currentTimeMillis()+"");
            //兼容Android Q和以下版本
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //android Q中不再使用DATA字段，而用RELATIVE_PATH代替
                //TODO RELATIVE_PATH是相对路径不是绝对路径;照片存储的地方为：内部存储/Pictures/preventpro
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/preventpro");
            }
            //设置文件类型
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/JPEG");
            //执行insert操作，向系统文件夹中添加文件
            //EXTERNAL_CONTENT_URI代表外部存储器，该值不变
            uri = mContext.get().getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        } else {
            String imageFileName = String.format("%s.jpg", System.currentTimeMillis());
            File storageDir = mContext.get().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File tempFile = new File(storageDir, imageFileName);
            uri = FileProvider.getUriForFile(mContext.get(),
                    mCaptureStrategy.authority, tempFile);
        }
        return uri;

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = String.format("%s.jpg", System.currentTimeMillis());
        File storageDir;
        if (mCaptureStrategy.isPublic) {//TODO 图片存在公共区域内部存储/Pictures
            //目录地址：内部存储/Pictures
            storageDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES);
            if (!storageDir.exists()) storageDir.mkdirs();
        } else {//TODO 图片存到沙箱内 getExternalFilesDir
            storageDir = mContext.get().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        }
        if (mCaptureStrategy.directory != null) {
            storageDir = new File(storageDir, mCaptureStrategy.directory);
            if (!storageDir.exists()) storageDir.mkdirs();
        }

        // Avoid joining path components manually
        File tempFile = new File(storageDir, imageFileName);

        // Handle the situation that user's external storage is not ready
        if (!Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(tempFile))) {
            return null;
        }

        return tempFile;
    }

    public Uri getCurrentPhotoUri() {
        return mCurrentPhotoUri;
    }

    public String getCurrentPhotoPath() {
        return mCurrentPhotoPath;
    }

    public void setDefaultPath(String defaultPath) {
        this.defaultPath = defaultPath;
    }
}
