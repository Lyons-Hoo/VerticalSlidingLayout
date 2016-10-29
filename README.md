# VerticalSlidingLayout
这是一个可以上下滑动的ViewGroup

使用方式：

      在你的layout.xml直接作为容器包裹子View

例如：

<com.example.lyons.demo.customerview.VerticalSlidingLayout
        android:id="@+id/vsl_test"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginLeft="10dp"
            android:background="@color/colorPrimary"
            android:text="这是第一个子View"
            android:textColor="#ffffff"
            android:textSize="30sp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginBottom="30dp"
            android:background="@color/colorAccent"
            android:text="这是第二个子View"
            android:textColor="#ffffff"
            android:textSize="20sp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:background="@color/colorPrimaryDark"
            android:text="这是第三个子View"
            android:textColor="#ffffff"
            android:textSize="20sp" />
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:background="@color/cardview_dark_background"
            android:text="这是第四个子View"
            android:textColor="#ffffff"
            android:textSize="20sp" />
    </com.example.lyons.demo.customerview.VerticalSlidingLayout>
    
        
        在你需要的地方直接像平时一样FindViewById即可：
    
    例如：
    
    /**
         * 设置监听
         */
        vsl.setOnViewIndexChangeListener(new SlidingViewGroup.OnViewIndexChangeListener() {
            @Override
            public void viewIndexChanged(int viewIndex) {
                /**
                 * 这里做你的逻辑操作（viewIndex为当前屏幕所呈现的View在容器中的下标）
                 */
                Toast.makeText(Main2Activity.this, "CurrentViewIndex:" + viewIndex, Toast.LENGTH_SHORT).show();
            }
        });
        
        
    然后就可以运行你的工程查看效果了
