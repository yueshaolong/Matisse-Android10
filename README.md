### 权限
The library requires two permissions:
- `android.permission.READ_EXTERNAL_STORAGE`
- `android.permission.WRITE_EXTERNAL_STORAGE`
- `android.permission.CAMERA`

### 使用

#### 指定默认打开的图片文件夹
```java
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
                        .forResult(REQUEST_CODE_CHOOSE);
```

#### 单独调用拍照功能
```java
Matisse.from(SampleActivity.this)
                        .choose(MimeType.of(MimeType.JPEG, MimeType.PNG))
                        .defaultPath(getPictureDirPath().getAbsolutePath())//设置拍照后照片存放的路径
                        .takePic(true)//设置立即拍照，跳转到系统相机界面
                        .capture(true)
                        .captureStrategy(
                                new CaptureStrategy(true, "com.zhihu.matisse.sample.fileprovider",
                                        //TODO 拍照后照片存得地址：内部存储/Pictures/preventpro；这个名字就是图片要存的文件夹名字
                                        "preventpro"))
                        .forResult(REQUEST_CODE_TAKE_PHOTO);
```
其中的CaptureStrategy的第二个参数作者是要和清单文件中的FileProvider中的authorities保持一致。
拍照完成后,记得扫描文件：
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            ...
            case REQUEST_CODE_TAKE_PHOTO:
                //拍照回来，刷新图库
                new SingleMediaScanner(this.getApplicationContext(),
                        getPictureDirPath().getAbsolutePath(),
                        new SingleMediaScanner.ScanListener() {
                    @Override public void onScanFinish() {
                        Log.i("SingleMediaScanner", "scan finish!");
                    }
                });
                break;
            ...
        }

    }
```