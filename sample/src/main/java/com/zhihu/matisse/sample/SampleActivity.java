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
package com.zhihu.matisse.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.engine.impl.PicassoEngine;
import com.zhihu.matisse.filter.Filter;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.io.File;
import java.util.List;

import io.reactivex.disposables.Disposable;

public class SampleActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE_CHOOSE = 23;
    private static final int REQUEST_CODE_TAKE_PHOTO = 44;

    private UriAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.zhihu).setOnClickListener(this);
        findViewById(R.id.zhihu2_all).setOnClickListener(this);
        findViewById(R.id.dracula).setOnClickListener(this);
        findViewById(R.id.only_gif).setOnClickListener(this);
        findViewById(R.id.take_photo).setOnClickListener(this);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter = new UriAdapter());

//        RxPermissions rxPermissions = new RxPermissions(this);
//        Disposable subscribe = rxPermissions.request(android.permission.READ_MEDIA_IMAGES)
//                .subscribe(granted -> {
//
//                });
    }

    // <editor-fold defaultstate="collapsed" desc="onClick">
    @SuppressLint("CheckResult")
    @Override
    public void onClick(final View v) {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                        startAction(v);
                    } else {
                        Toast.makeText(SampleActivity.this, R.string.permission_request_denied, Toast.LENGTH_LONG)
                                .show();
                    }
                }, Throwable::printStackTrace);
    }
    // </editor-fold>

    private void startAction(View v) {
        switch (v.getId()) {
            case R.id.take_photo://这就是直接拍照的功能
                Matisse.from(SampleActivity.this)
                        .choose(MimeType.of(MimeType.JPEG, MimeType.PNG))
                        .defaultPath(getPictureDirPath(false).getAbsolutePath())//设置拍照后照片存放的路径
                        .takePic(true)//设置立即拍照，跳转到系统相机界面
                        .capture(true)
                        .captureStrategy(
                                new CaptureStrategy(false, "com.zhihu.matisse.sample.fileprovider",
                                        //TODO 拍照后照片存得地址：内部存储/Pictures/preventpro；这个名字就是图片要存的文件夹名字
                                        ""))
                        .forResult(REQUEST_CODE_TAKE_PHOTO);
                break;
            case R.id.zhihu:
                Matisse.from(SampleActivity.this)
                        .choose(MimeType.ofImage(), false)
                        .countable(true)
                        .capture(true)
                        .captureStrategy(
                                //TODO 第一个参数isPublic表示可以存储到公共区域还是沙箱内，是对Android10的适配；在MediaStoreCompat中使用
                                new CaptureStrategy(false, "com.zhihu.matisse.sample.fileprovider", ""))
                        .maxSelectable(9)
                        .addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                        .gridExpectedSize(
                                getResources().getDimensionPixelSize(R.dimen.grid_expected_size))
                        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                        .thumbnailScale(0.85f)
                        .imageEngine(new GlideEngine())
                        .setOnSelectedListener((uriList, pathList) -> {
                            Log.e("onSelected", "onSelected: pathList=" + pathList);
                        })
                        .showSingleMediaType(true)
                        .originalEnable(true)
                        .maxOriginalSize(10)
                        .autoHideToolbarOnSingleTap(true)
                        .setOnCheckedListener(isChecked -> {
                            Log.e("isChecked", "onCheck: isChecked=" + isChecked);
                        })
                        .defaultPath(getPictureDirPath(false).getAbsolutePath())//设置默认打开照片的路径
                        .forResult(REQUEST_CODE_CHOOSE);
                break;
            case R.id.zhihu2_all:
                Matisse.from(SampleActivity.this)
                        .choose(MimeType.ofImage(), false)
                        .countable(true)
                        .capture(true)
                        .captureStrategy(
                                //TODO 第一个参数isPublic表示可以存储到公共区域还是沙箱内，是对Android10的适配；在MediaStoreCompat中使用
                                new CaptureStrategy(true, "com.zhihu.matisse.sample.fileprovider", "preventpro"))
                        .maxSelectable(9)
                        .addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                        .gridExpectedSize(
                                getResources().getDimensionPixelSize(R.dimen.grid_expected_size))
                        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                        .thumbnailScale(0.85f)
                        .imageEngine(new GlideEngine())
                        .setOnSelectedListener((uriList, pathList) -> {
                            Log.e("onSelected", "onSelected: pathList=" + pathList);
                        })
                        .showSingleMediaType(true)
                        .originalEnable(true)
                        .maxOriginalSize(10)
                        .autoHideToolbarOnSingleTap(true)
                        .setOnCheckedListener(isChecked -> {
                            Log.e("isChecked", "onCheck: isChecked=" + isChecked);
                        })
                        .defaultPath(null)//设置默认打开照片的路径,null表示展示全部的照片
                        .forResult(REQUEST_CODE_CHOOSE);
                break;
            case R.id.dracula:
                Matisse.from(SampleActivity.this)
                        .choose(MimeType.ofImage())
                        .theme(R.style.Matisse_Dracula)
                        .countable(false)
                        .addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                        .maxSelectable(9)
                        .originalEnable(true)
                        .maxOriginalSize(10)
                        .imageEngine(new PicassoEngine())
                        .forResult(REQUEST_CODE_CHOOSE);
                break;
            case R.id.only_gif:
                Matisse.from(SampleActivity.this)
                        .choose(MimeType.of(MimeType.GIF), false)
                        .countable(true)
                        .maxSelectable(9)
                        .addFilter(new GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                        .gridExpectedSize(
                                getResources().getDimensionPixelSize(R.dimen.grid_expected_size))
                        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                        .thumbnailScale(0.85f)
                        .imageEngine(new GlideEngine())
                        .showSingleMediaType(true)
                        .originalEnable(true)
                        .maxOriginalSize(10)
                        .autoHideToolbarOnSingleTap(true)
                        .forResult(REQUEST_CODE_CHOOSE);
                break;
            default:
                break;
        }
        mAdapter.setData(null, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("-------->"+requestCode+"===="+resultCode+"-----"+data);
        if (/*requestCode == REQUEST_CODE_CHOOSE &&*/ resultCode == RESULT_OK) {
            mAdapter.setData(Matisse.obtainResult(data), Matisse.obtainPathResult(data));
            Log.e("OnActivityResult ", String.valueOf(Matisse.obtainOriginalState(data)));
        }
    }

    private static class UriAdapter extends RecyclerView.Adapter<UriAdapter.UriViewHolder> {

        private List<Uri> mUris;
        private List<String> mPaths;

        void setData(List<Uri> uris, List<String> paths) {
            mUris = uris;
            mPaths = paths;
            notifyDataSetChanged();
        }

        @Override
        public UriViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new UriViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.uri_item, parent, false));
        }

        @Override
        public void onBindViewHolder(UriViewHolder holder, int position) {
            holder.mUri.setText(mUris.get(position).toString());
            holder.mPath.setText(mPaths.get(position));

            holder.mUri.setAlpha(position % 2 == 0 ? 1.0f : 0.54f);
            holder.mPath.setAlpha(position % 2 == 0 ? 1.0f : 0.54f);
        }

        @Override
        public int getItemCount() {
            return mUris == null ? 0 : mUris.size();
        }

        static class UriViewHolder extends RecyclerView.ViewHolder {

            private TextView mUri;
            private TextView mPath;

            UriViewHolder(View contentView) {
                super(contentView);
                mUri = (TextView) contentView.findViewById(R.id.uri);
                mPath = (TextView) contentView.findViewById(R.id.path);
            }
        }
    }
    /**
     * 设置自己存放照片的路径
     * @return
     */
    public File getPictureDirPath(boolean isPublic) {
        File mIVMSFolder = null;
        try {
            if (isPublic) {
                //设置图片存储的位置：内部存储/Pictures/preventpro
                String path = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).getAbsolutePath() + File.separator + "preventpro";
                mIVMSFolder = new File(path);
                if (!mIVMSFolder.exists()) {
                    mIVMSFolder.mkdirs();
                }
            } else {
                mIVMSFolder = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mIVMSFolder;
    }
}
