package com.sd.lib.retry;

import android.os.Handler;
import android.os.Looper;

/**
 * 重试帮助类
 */
public abstract class FRetryHandler
{
    /**
     * 最大重试次数
     */
    private final int mMaxRetryCount;

    /**
     * 重试是否已经开始
     */
    private boolean mIsStarted;
    /**
     * 当前第几次重试
     */
    private int mRetryCount;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public FRetryHandler(int maxRetryCount)
    {
        if (maxRetryCount <= 0)
            throw new IllegalArgumentException("maxRetryCount must > 0");
        mMaxRetryCount = maxRetryCount;
    }

    /**
     * 是否已经开始重试
     *
     * @return
     */
    public final boolean isStarted()
    {
        return mIsStarted;
    }

    /**
     * 返回当前第几次重试
     *
     * @return
     */
    public final int getRetryCount()
    {
        return mRetryCount;
    }

    /**
     * 开始重试
     */
    public final synchronized void start()
    {
        if (mIsStarted)
            return;

        mRetryCount = 0;
        setStarted(true);

        retry(0);
    }

    /**
     * 重试，只有{@link #isStarted()}为true，此方法才有效
     *
     * @param delayMillis 延迟多少毫秒
     * @return true-成功发起了一次重试
     */
    public final synchronized boolean retry(long delayMillis)
    {
        if (!mIsStarted)
            return false;

        if (checkMaxRetry())
            return false;

        mHandler.removeCallbacks(mRetryRunnable);
        mHandler.postDelayed(mRetryRunnable, delayMillis);
        return true;
    }

    private final Runnable mRetryRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            synchronized (FRetryHandler.this)
            {
                if (!mIsStarted)
                    return;

                mRetryCount++;
                onRetry();

                checkMaxRetry();
            }
        }
    };

    /**
     * 是否达到最大重试次数，如果达到会停止重试，并回调{@link #onRetryMaxCount()}方法
     *
     * @return true-达到最大次数
     */
    protected final synchronized boolean checkMaxRetry()
    {
        if (mRetryCount >= mMaxRetryCount)
        {
            // 达到最大重试次数
            stop();
            onRetryMaxCount();
            return true;
        }
        return false;
    }

    /**
     * 停止重试
     */
    public final synchronized void stop()
    {
        mHandler.removeCallbacks(mRetryRunnable);
        mRetryCount = 0;
        setStarted(false);
    }

    private void setStarted(boolean started)
    {
        if (mIsStarted != started)
        {
            mIsStarted = started;
            onStateChanged(started);
        }
    }

    protected void onStateChanged(boolean started)
    {
    }

    /**
     * 执行重试任务（UI线程）
     */
    protected abstract void onRetry();

    /**
     * 达到最大重试次数
     */
    protected void onRetryMaxCount()
    {
    }
}